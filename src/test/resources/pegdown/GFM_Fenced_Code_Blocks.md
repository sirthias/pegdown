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

and here is another one:

```java
public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return CommonBundle.message(BUNDLE, key, params);
}

public static String messageOrBlank(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return CommonBundle.messageOrDefault(BUNDLE, key, "", params);
}
```

test opening with more than 3 ticks

````java
public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return CommonBundle.message(BUNDLE, key, params);
}

public static String messageOrBlank(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return CommonBundle.messageOrDefault(BUNDLE, key, "", params);
}
```
should still be fenced
`````
should still be fenced
````