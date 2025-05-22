# RoosterEnthusiasts Mobile App

## Overview

RoosterEnthusiasts is a mobile application designed to connect farmers and consumers in a
marketplace for poultry products. The app facilitates user authentication, product listings, order
management, and community engagement, with a focus on traceability and user feedback.

## Architecture

The project follows the **MVVM (Model-View-ViewModel)** pattern, fully integrated with a
`BaseViewModel` and `Resource` pattern for state management. Key architectural components include:

- **Modularization**: The project is structured into distinct modules:
   - `:app` - Main application module.
   - `:core` - Core utilities and base components.
   - `:community` - Features for community engagement and posts.
   - `:traceability` - Functionality for tracking product origins.
   - `:marketplace` - Product listing and creation features.
   - `:orders` - Order management and tracking.
   - `:auth` - User authentication and role-based navigation.
- **State Management**: Utilizes an event-state-effect pattern across ViewModels for consistent
  state handling.
- **Dependency Management**: Centralized using Gradle version catalog for streamlined dependency
  updates.
- **Testing Infrastructure**: Includes Jacoco for coverage reports and Turbine for Flow testing.

## Features

### Authentication Module

- Complete flow with Firebase and Parse integration for user authentication.
- Role-based navigation for Farmers and Consumers with dedicated UI screens (`LoginScreen`,
  `SignupScreen`).

### Marketplace Functionality

- Core product listing and creation features with domain models, repositories, and use cases.
- Initial UI setup for product listing via `ProductListScreen`, integrated into navigation.

### Orders Management

- Refactored `OrderViewModel` to align with `BaseViewModel` pattern using a custom `CoroutineScope`.
- UI components for order listing (`OrderListScreen`) and details (`OrderDetailsScreen`) implemented
  with Compose.
- Order status updates UI for sellers in `OrderDetailsScreen`.

### Profile Management

- Refactored `ProfileViewModel` with custom `CoroutineScope` for coroutine handling.
- Profile UI via `ProfileScreen` to display user information and feedback.
- Edit profile functionality with `EditProfileScreen` for updating user details and image uploads.

### Navigation

- Updated app navigation graph to include routes for orders, profile, and edit profile screens,
  ensuring seamless user flow.
- Defined in `Navigation.kt` with role-based start destinations (e.g., `Home` for farmers,
  `Marketplace` for consumers).

## Technical Debts

- **IDE Sync Issues**: Persistent Gradle sync issues need resolution to clear linter errors related
  to dependencies.
- **Incomplete UI Features**: Some UI components (e.g., order status updates, edit profile) require
  further implementation or refinement.
- **Testing Gaps**: Additional UI and integration tests needed for new components to ensure
  reliability.

## Recent Development Milestones

- **ViewModel Refactor**: Completed for Orders and Profile management.
- **UI Integration**: Added Orders and Profile screens.
- **Navigation Updates**: Integrated routes for new screens.

## Pending Tasks

- Implement comprehensive UI for order status updates.
- Complete edit profile functionality with full validation and error handling.
- Add comprehensive testing for new features, including unit and UI tests.

## Setup Instructions

### Prerequisites

- Android Studio with Kotlin plugin.
- Gradle version compatible with the project (check `gradle/wrapper/gradle-wrapper.properties`).
- Firebase and Parse SDK configurations.

### Installation

1. Clone the repository to your local machine.
2. Open the project in Android Studio.
3. Sync the project with Gradle to resolve dependencies.
4. Configure Firebase and Parse credentials:
   - Add `google-services.json` to the `app` directory for Firebase.
   - Ensure Parse initialization in the app's `Application` class or main activity.
5. Build and run the app on an emulator or physical device.

### Testing

- Unit tests are located in `app/src/test/java` for ViewModels like `OrderViewModel` and
  `ProfileViewModel`.
- Run tests with `./gradlew test` from the project root.
- Coverage reports can be generated with `./gradlew jacocoTestReport`.

## Project Structure

```
RoosterEnthusiasts/
│
├── app/                    # Main application module
│   ├── src/main/java/      # Main source code
│   │   ├── com/example/mvp/ui/                # UI components and screens
│   │   ├── com/example/mvp/data/              # Data models and repositories
│   │   └── com/example/mvp/navigation/        # Navigation setup
│   └── build.gradle.kts    # App module build script
│
├── core/                   # Core utilities and base components
├── community/              # Community features
├── traceability/           # Traceability features
├── marketplace/            # Marketplace features
├── orders/                 # Orders management
├── auth/                   # Authentication module
│
├── gradle/                 # Gradle wrapper and configuration
└── build.gradle.kts        # Top-level build script
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/your-feature`).
3. Commit your changes (`git commit -m 'Add your feature'`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a pull request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.