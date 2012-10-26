A fenced code block:

```scala
class Example(name: String) {
  val field: Option[Int] = None
}
```

And another one, this time with an empty line:

```javascript
   var model = ko.sync.Model({
       dataStore: store,
       dataTable: table,
       fields: {counter: 0}
   });

   var data = ko.sync.use({}, model);
   data.crud.isDirty(); // false
   data.counter(1);
   data.crud.isDirty(); // true
```

and some more content....