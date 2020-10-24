## QAuto - A library for autowiring.

### Installation
Put the dependency in your build script:  

**Maven:**
```xml
<dependency>
  <groupId>com.azoraqua</groupId>
  <artifactId>QAuto</artifactId>
  <version>VERSION</version>
</dependency>
```

**Gradle:**
```groovy
  QAuto 'com.azoraqua:QAuto:VERSION'
```

### Usage
- Create a bean using `@Bean` on a method.
- Create an autowireable field with `@Autowired`.
- Invoke the `ClassScanner.scan()` and `ClassScanner.process()` methods.