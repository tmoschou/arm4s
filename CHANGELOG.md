# Changelog

## [1.1.0-SNAPSHOT](https://github.com/tmoschou/arm4s/tree/develop) (Unreleased)
- Added cross build for Scala 2.13-M5
- Support explicitly passing the CanManage object to `ImplicitManageable`'s methods [#5](https://github.com/tmoschou/arm4s/issues/5)
- `CanManage`'s `onFinally` and`onException` now defaults to no-op implementation [#6](https://github.com/tmoschou/arm4s/issues/6) 

## [1.0.0](https://github.com/tmoschou/arm4s/releases/tag/v1.0.0) (2018-09-27)
New API Improvements [#3](https://github.com/tmoschou/arm4s/pull/3): 
- CanManage/DefaultManagedResource now supports on-exception lifecycle hooks
- Implicit decorator methods to now explicitly convert a resource into a 
managed one, over implicit conversions.

## [0.2.0](https://github.com/tmoschou/arm4s/releases/tag/v0.2.0) (2017-01-30)
- Move implicits package-object to Implicits object. [#1](https://github.com/tmoschou/arm4s/issues/1) 

## [0.1.0](https://github.com/tmoschou/arm4s/releases/tag/v0.1.0) (2017-01-12)
- Initial release
