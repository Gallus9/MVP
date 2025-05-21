# MVP Marketplace App

## Project Overview

The MVP Marketplace App is a mobile application that connects farmers directly with consumers. It
enables farmers to list their products for sale and allows general users to browse, purchase, and
track orders. The application is built using modern Android development practices with Jetpack
Compose for the UI and Parse SDK for backend interactions.

## Architecture

### Backend

The application uses Parse Server as a backend solution with the following models:

1. **User**
    - Custom `User` class extending `ParseUser`
    - Supports different roles (Farmer, GeneralUser)
    - Integrated with Firebase Authentication
    - Stores profile information and images

2. **Products**
    - `ProductListing` model for marketplace items
    - Support for traceability features
    - Related media (images) through Parse relationships

3. **Orders**
    - `Order` model that tracks transactions between buyers and sellers
    - Status tracking (Pending, Confirmed, Shipped, Delivered, Completed, Cancelled)
    - Associations with products, buyers, and sellers

4. **Feedback**
    - User-to-user feedback (`Feedback` model)
    - Product feedback (`ProductFeedback` model)
    - Rating system and comments

5. **Media**
    - Supports both images and videos
    - Linked to products and user profiles

### Frontend

The frontend is built with Jetpack Compose following MVVM architecture:

1. **ViewModels**
    - `AuthViewModel`: Handles user authentication operations
    - `ProductViewModel`: Manages product listings, creation, and fetching
    - `OrderViewModel`: Handles order operations and status updates
    - `ProfileViewModel`: Manages user profile data and feedback

2. **Navigation**
    - Navigation graph with multiple destinations
    - Bottom navigation for main app sections
    - Deep links for product details

3. **Screens**
    - Auth: Login, Signup
    - Home: Featured products, welcome information
    - Marketplace: Product listings, product details
    - Orders: Order history, order details
    - Profile: User information, feedback, ratings

4. **Components**
    - Product card for consistent product display
    - Featured products carousel
    - Order status badges
    - Feedback display components

## Implementation Status

### ✅ Core Configuration

- Firebase Authentication integrated
- Parse initialization with local datastore enabled
- Parse models registered for all data types

### ✅ Authentication Integration

- User registration creates accounts in both Firebase and Parse
- Login authenticates with both systems
- Session management synchronized between platforms
- User roles (Farmer/General User) properly enforced

### ✅ Data Binding – API Layer

- Repository pattern implemented for all data types
- MediaRepository for handling file uploads
- ProductRepository for marketplace listings
- UserRepository for user management
- OrderRepository for transaction handling

### ✅ Realtime Messaging

- ChatRepository using Firebase Realtime Database
- Message sending and receiving implemented
- Conversation management and tracking

### ✅ Role-Based Navigation

- Different navigation paths for Farmers vs. General Users
- Dynamic bottom navigation based on user role
- Access control for role-specific screens

### ✅ Security & Rules

- Firebase Realtime Database rules implemented
- Parse ACL setup for proper object permissions

## Installation

Ensure that you have Android Studio and the required SDKs installed, then:

1. Clone this repository
2. Add required configuration for Parse Server and Firebase in your `local.properties` or
   environment
3. Open the project in Android Studio
4. Connect a device or start an emulator
5. Run the app through Android Studio

## Testing

Run the test suite to verify functionality:
