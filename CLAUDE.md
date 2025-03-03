# FT_HANGOUTS Project Guidelines

## Build & Test Commands
- Build project: `./gradlew build`
- Run app: `./gradlew installDebug` (device/emulator required)
- Run all tests: `./gradlew test`
- Run single unit test: `./gradlew test --tests "com.example.ft_hangouts.ExampleUnitTest.addition_isCorrect"`
- Run instrumented tests: `./gradlew connectedAndroidTest`
- Lint check: `./gradlew lint`

## Code Style Guidelines
- **Naming Conventions**: 
  - Classes: PascalCase (e.g., `MainActivity`)
  - Variables/methods: camelCase (e.g., `mAppBarConfiguration`)
  - Constants: UPPER_SNAKE_CASE
  - Layout files: snake_case (e.g., `activity_main.xml`)
- **Imports**: Group Android imports first, then third-party, then Java standard library
- **Error Handling**: Use try/catch blocks for exceptions, log errors with descriptive messages
- **ViewBinding**: Use ViewBinding pattern for UI interaction, not findViewById
- **Architecture**: Follow MVVM pattern with ViewModels and LiveData
- **Comments**: Add comments for complex logic, no comments for obvious code