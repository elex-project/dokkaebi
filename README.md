# Project Dokkaebi

Google Analytics Measurement Protocol

https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide

```java
StopWatch watch = StopWatch.createStarted();

Tracker tracker = Tracker.getInstance("UA-xxxxxxxx-x", UUID.randomUUID());
tracker.appStart("Dokkaebi", "Test", "com.elex_project.dokkaebi");
tracker.trackScreen("Main");

tracker.trackTiming("test", "test", "test", watch.getTime());

tracker.appEnd();
```

---

developed by Elex

https://www.elex-project.com
