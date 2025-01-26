# Facing Migrations with Respect

The day has arrived, after avoiding it like the plague, there's nowhere left to hide. The big
scary database migration that has been off on the horizon is finally at the front gates of development.
The database migration is the process of reshaping schema and data within a projects' database, and because
a database can be the backbone of a project, migrations are
acutely prone to causing unintended behavior in the system. It can be anywhere from fixable errors
to unrecoverable data. Needless to say, migrations need to be treated with respect and care, otherwise
they will retaliate with dire consequences.

## Preface

### c3kit
In this article, the namespace [`c3kit.bucket.migrator`](https://github.com/cleancoders/c3kit-bucket/blob/master/src/cljc/c3kit/bucket/migrator.cljc) 
, [`c3kit.bucket.api`](https://github.com/cleancoders/c3kit-bucket/blob/master/src/cljc/c3kit/bucket/api.cljc), and
[`c3kit.apron.legend`](https://github.com/cleancoders/c3kit-apron/blob/master/src/cljc/c3kit/apron/legend.cljc)
will be used as part of the clojure library [c3kit-bucket](https://github.com/cleancoders/c3kit-bucket) and
[c3kit-apron](https://github.com/cleancoders/c3kit-apron).

### Schema
Additionally, the use of [`c3kit.apron.schema`](https://github.com/cleancoders/c3kit-apron/blob/master/src/cljc/c3kit/apron/schema.cljc)
is utilized to validate table structure in the database. If unfamiliar, take a look at the blog post on this topic:
[Control Clojure Data with Schema](https://cleancoders.com/blog/2024-04-20-c3kit-schema-library)

## Risk Assessment

The practices that will be taught in this article come at the cost of time and effort. If every migration
is treated with utmost caution, even the simplest of changes can be extended from minutes to hours or days
of effort. Therefore, to avoid expending energy where it's not always necessary, risk assessment is key for
approaching any migration.

### High Risk
When executing a migration, it is typically obvious which migrations are high risk, but it is still important
to understand the distinction. High risk occurs when the migration requires any one of the following:

1. data is being deleted
2. data is being moved or modified
3. columns are being altered

High-risk migrations demand careful attention, as neglecting them can lead to significant repercussions. 
To mitigate potential issues, such migrations require thorough testing, isolation, and close monitoring.
Risk assessment should always lean on the side of caution—once a migration is deployed, reversing it may not be 
an option. When in doubt, assume the highest possible risk.

### Low Risk

On the other end, tells of low risk include:

1. the existing data is untouched
2. the existing data is easily replaceable or unimportant

These types of migrations are typically simpler to execute. A common example is adding a new column or table that 
wasn’t present before. While it’s less frequent, there are cases where the data involved may be less critical. 
As a best practice, even replaceable data should be handled with the same care as other data. However, if the
effort required to safely migrate the data outweighs the effort needed to replace any potential losses, the risk 
level of the migration can be adjusted accordingly.

## Temporally Decoupling a Migration
### Organizing Migrations Chronologically
When making a migration, it's key to understand that migrations by default are dependent on the state of the project and
database at the time that the migration is created.
If a project is actively in development, the structure of the database changes over time. One column is renamed, another
removed entirely, and so on and so forth. Consider the following two migrations:

```clojure
(ns db.migrations.dog-name-add
  (:require [c3kit.bucket.migrator :as m]))

(defn up []
  (m/add-attribute! :dog :name {:type :string}))
```

```clojure
(ns db.migrations.dog-name-remove
  (:require [c3kit.bucket.migrator :as m]))

(defn up []
  (m/remove-attribute! :dog :name))
```

The order that these migrations run will determine the schema of `Dog` in the database. If `dog-name-add` is run
first while `dog-name-remove` second, then the ending schema will not have the `name` column. If the order
is reversed, the `name` column will remain present. Therefore, it is good practice to keep a timestamp in the namespace.
This will keep the files organized in proper chronological order.

![image](/images/chronological-names.png)

### Migration Reversals

To maintain easy traversal, the "end state" of any migration should be reachable from any other migration
through the form of reversals or `down` functions. Migrations should not be one way paths, instead they should allow 
movement in both directions. Down migrations should have the purpose of both reverting
the schema, and the data. By doing so, it provides an emergency break-glass rewind on the database state without the 
need for a backup.

```clojure
; installed schema
(def dog
  {:kind (s/kind :dog)
   :id   s/id
   :name {:type :string}})

===================

(defn up []
  (m/add-attribute! :dog :name {:type :string})
  (db/tx {:kind :dog :name "fido"}))

(defn down []
  (let [fido (db/ffind-by :dog :name "fido")]
    (db/delete fido)
    (m/remove-attribute! :dog :name)))
```

In this example, the up migration will add the `name` attribute, and also create a new database entry for Fido.
Then the down migration will reverse the effects by deleting Fido and removing the name attribute. This puts the
database back to the state that it was just before this migration was run.

### Schema Isolation
While database holds its own schema, the schema used for data validation exists within the source code. While migrations
are meant to be left unchanged, the same is not true for source code schema. Because of this, migrations are
vulnerable to their dependency on the current structure of schema. The need for schema isolation can vary depending
on the database. Typically, datomic is very robust with handling mismatched schema, therefore this does not often
become a problem. However, other database implementations like Postgres can be highly dependent on the current
schema. Consider Fido now existing in a Postgres database.
```clojure
(def dog
  {:kind (s/kind :dog)
   :id   {:type :uuid :db {:type "uuid PRIMARY KEY"} :strategy :pre-populated}
   :name {:type :string :db {:type "text"}}})

===================

(defn up []
  (db/tx {:kind :dog :name "fido" :id (ccc/new-uuid)}))

(defn down []
  (let [fido (db/ffind-by :dog :name "fido")]
    (when fido (db/delete fido))))
```
The migration will work without a problem, but because fido was created using a name type,
this migration now becomes coupled to the current state of the schema with the `name` attribute.
It is possible that at some point, the name column will be removed and therefore if following good practice, the schema
will reflect that removal. This will cause the migration to not work as intended, highlighting its fragility. So what
can be improved?
```clojure
(def migration-dog
  {:kind (s/kind :dog)
   :id   {:type :uuid :db {:type "uuid PRIMARY KEY"} :strategy :pre-populated}
   :name {:type :string :db {:type "text"}}})

(def schemas [migration-dog])
(def migration-db (db/create-db config/jdbc schemas))

(defn up []
  (db/tx- migration-db {:kind :dog :name "fido" :id (ccc/new-uuid)}))

(defn down []
  (let [fido (db/ffind-by- migration-db :dog :name "fido")]
    (when fido (db/delete- migration-db fido))))
```
With this approach, the schema for dog is defined within the migration namespace, ensuring that it is unique to
the migration itself. Then, the database implementation is established using the `migration-dog` schema. By doing this, the
`name` attribute is guaranteed to exist for this migration, regardless of how the shared schema is altered.

Note how `db/tx`->`db/tx-`, `db/ffind-by`->`db/ffind-by-`, and `db/delete`->`db/delete-`. These functions within
`c3kit.bucket.api` behave similarly, but expect an explicit database implementation, allowing for more flexibility
with the type of database and what schemas are installed into the legend.

## Testing Migration Code

### Manual Testing
Before pushing a migration, always ensure it works on the local environment first. This includes both the
up and down migrations. For simple migrations like adding a column, some manual testing is typically
all that is needed. It can be especially beneficial to create data that would test the edge cases of the
migration. Local databases hold replaceable data unliked production environments. If anything goes wrong,
make sure it stays on the local environment.

### Unit Tests
When in doubt, unit tests can be one of the best ways to have confidence in a high risk migration.
If the functions needed to run the migration are complicated, they should be treated like any other
source code. Therefore, lets make some tests for the previous example
```clojure
(describe "Add Fido Migration"
  (context "up"
    (it "fido exists"
      (sut/up)
      (should= "fido" (:name (db/ffind-by :dog :name "fido")))))

  (context "down"
    (it "fido is deleted"
      (sut/down)
      (should-be-nil (db/ffind-by :dog :name "fido")))))
```
Once the expected behavior is reflected in the tests, the migration is ready for manual testing, then
deployment.

#### Isolated Schemas and Tests
When using schema isolation, that will require a little extra work for the tests as well.
If left unchanged, these tests for adding Fido will not use the `:memory` implementation.
As previously mentioned, the functions in the migration use an explicit database . 
This means the `:jdbc` database becomes what is explicitly used in the test.
To fix this, `redefs-around` becomes quite handy.
```clojure
(defn create-spec-db [schemas]
  (let [backend-schemas  (remove #(contains? schemas (-> % :kind :value)) schema/full-schema)
        schemas (concat backend-schemas schemas)
        spec-db (db/create-db (assoc config/jdbc :impl :memory) [schemas])
        _ (db/set-safety! false)]
    spec-db))

(def spec-db (create-spec-db sut/schemas))

(describe "Dog name add"
  (redefs-around [sut/migration-db spec-db])
  (context "up"
    (it "fido exists"
      (sut/up)
      (should= "fido" (:name (db/ffind-by- spec-db :dog :name "fido")))))

  (context "down"
    (it "fido is deleted"
      (sut/down)
      (should-be-nil (db/ffind-by- spec-db :dog :name "fido")))))
```
A lot was added here so let's unpack each change:
1. `create-spec-db`: This function will expect the schemas that is intended for use and return an established
`:memory` database implementation using those schemas. Additionally, the database safety flag is turned
off because clearing the database between each test can come in handy.
2. `spec-db`: This is what holds the established database from `create-spec-db`, notice how the schemas used
are the isolated schemas from the migration namespace.
3. `redefs-around`: Before the tests are ready to run, the migration namespace needs to use the `spec-db`, 
not the `jdbc-db` implementation. This `redefs-around` will do just that.
4. `db/ffind-by-`: Like in the migration namespace, the tests now need to use the explicit `spec-db` when
making any queries.

Not only are these tests now compatible with the isolated schemas migration, they're also much more robust.
Similar to migrations, tests are just as susceptible to failing when the common schemas are changed.
With this approach, that is no longer an issue for them because they rely on the same temporally
decoupled schemas that the migration does.

## Safety Guidelines

To have a safe migration is to preserve the functionality of the service after execution.
If data is lost or the system isn't ready for the changes made, then proper safety guidelines have not
been followed.

### Creation, Modification, Deletion
When actually creating a migration, it is important to make changes in the order of creation, modification,
then deletion.

```clojure
(defn up []
  (m/add-attribute! "dog" "age" {:type :long})
  (let [dogs (db/find-by- migration-db :dog)
        updated-dogs (map #(assoc % :age (count (:name %))) dogs)]
    (db/tx* migration-db updated-dogs))
  (m/remove-attribute! "dog" "name"))
```
In this example, the migration that occurred was removing the name column, and creating an age column which depends
on the number of characters of the dogs name. This migration format complies with create, modify, and delete:

1. The `age` column is added to the table. If a failure happens here, no changes will affect the existing database.
2. The `age` column is given data by means of counting the chars in the `name` column. If an failure happens here, then
`age` will be not filled or only partially filled with data.
3. The `name` column is deleted last. If a failure happens here, the `name` column is preserved with its data.

With this order, notice how existing data is never at risk of being lost at any point in the migration. Only once all
other steps of the migration are without error, will the `name` data be finally removed. Keep in mind that unit tests
are important for the modify step of this migration. If it does not fail, but does not behave in the way intended,
data is still at risk of being lost.

### Soft Deletion
In the last example, deleting the `name` column and being replaced by `age` is a risky move. The reason for this is
that there is not enough information to reverse this migration. Having an `age` of 4 could be reversed into "fido",
but just as easily "spot", or even "asdf". In situations like these where the migration is irreversible in nature,
having soft deletion is a perfect opportunity to exercise caution.
```clojure
(defn up []
  (m/add-attribute! "dog" "age" {:type :long})
  (let [dogs (db/find-by- migration-db :dog)
        updated-dogs (map #(assoc % :age (count (:name %))) dogs)]
    (db/tx* migration-db updated-dogs))
  (m/rename-attribute! "dog" "name" "dog" "name_deleted"))

(defn down []
  (m/rename-attribute! "dog" "name_deleted" "dog" "name")
  (m/remove-attribute! "dog" "age"))
```
This is the same migration as before, except the `name` data is preserved. Instead, the column is marked as deleted.
This can be helpful to indicate that the data should no longer be used, but is still accessible if needed.
Additionally, it is now possible to reverse the migration back to its before state. If the desired outcome
is to fully remove `name` from the database, using soft-deletion can be a good "toe in the lake" approach.
Roll out the migration with the column set aside from being used, and once the `name` column is confidently
not needed for the system to function, a full deletion can occur in a later migration.

### Is the System Ready for Deletion?
With the name->age example, there is another possible vulnerability. What if the rest of the
system is still dependent on `name`? This might be an easy question to answer on solo development projects, but
as it grows into a project with a team, or possibly even multiple teams, answering that question can be difficult.
It's never acceptable to push changes that stop the system from working the way it should. When making migrations
like these, do research and take the necessary steps needed to have assurance that `name` is no longer needed.
Be confident things are ready for soft-deletion or even full deletion. If this question cannot be answered, it is 
better to avoid deletion entirely. This decision at worst, leads to the database having extra unnecessary data.
```clojure
(defn up []
  (m/add-attribute! "dog" "age" {:type :long})
  (let [dogs (db/find-by- migration-db :dog)
        updated-dogs (map #(assoc % :age (count (:name %))) dogs)]
    (db/tx* migration-db updated-dogs)))

(defn down []
  (m/remove-attribute! "dog" "age"))
```

## Final Thoughts
Migrations are scary, there's no doubt about it. They will bite hard if underestimated. This article
serves as a lesson to view the implementation of a migration as more than just a few lines of code.
Migrations are a process, they need risk-assessment, research, and possibly many layers of caution to prevent
irreversible bugs. Keeps these things in mind when estimating for a migration. With "a few lines of code"
it's easy to say it'll take an hour at most. But the "process of safely migrating" will get a much more
accurate estimate which allows a developer to bring in the time, care, and respect that a migration needs.
