# Frontend Implementation Plan for Rooster Enthusiast MVP

This frontend plan is derived from the backend schema designed for Farmers and General Users using **Back4App (Parse)** and **Firebase**. The design follows a **modular architecture**, allowing AI agents or human developers to generate and integrate code efficiently with backend services.

---

## 1. User Roles & Navigation

### Roles Supported in MVP

* **General User**: Can explore, order, chat, and give feedback.
* **Farmer**: Can list products, manage listings, chat, and view feedback.

### Navigation Tabs (per Role)

| Role         | Nav Tabs                                 |
| ------------ | ---------------------------------------- |
| General User | Market, Explore, Create, Cart, Profile   |
| Farmer       | Home, Market, Create, Community, Profile |

---

## 2. Screens & Components (Mapped to Backend Classes)

### 🛒 Product Listing (Parse Class: `ProductListing`)

* **MarketPageScreen**

  * List products: GET `/ProductListing` (filter: `isTraceable`, `location`, `breed`)
  * Card Component: Image, Price, Verified Badge
  * Tap for details
* **ProductDetailScreen**

  * Fetch single product by ID: GET `/ProductListing/{objectId}` + include `seller`, `images`
  * Display product traceId, age, images, etc.
  * Button: Add to Cart / Contact Seller

### 📝 Listing (Farmer Only)

* **CreateListingScreen**

  * POST to `/ProductListing`
  * Input: `title`, `description`, `price`, `traceable`, `images`
  * Link images (uploaded via `/Media` class)

### 🧾 Orders (Parse Class: `Order`)

* **CartScreen**

  * Local cart management
  * POST `/Order` on confirmation
  * Input: `productId`, `quantity`, `price`, `buyer`
* **OrderHistoryScreen**

  * GET `/Order` (filter: `buyer == currentUser`)
  * Display: status, product info, time

### 👥 User Profile (Parse Class: `User`)

* **ProfileScreen**

  * Show user info (GET `/users/me`)
  * Display `firebaseUid`, `role`, profile photo
  * Enable: Update profile photo → upload to `Media`

---

## 3. Feedback (Parse Classes: `Feedback`, `ProductFeedback`)

* **LeaveFeedbackScreen**

  * POST `/Feedback`
  * Fields: `fromUser`, `toUser`, `rating`, `comment`, `order`
  * POST `/ProductFeedback` for product-specific rating
* **FeedbackDisplay**

  * GET `/Feedback` where `toUser == seller`
  * GET `/ProductFeedback` where `product == objectId`

---

## 4. Media Upload & Display (Parse Class: `Media`)

* **ImageUploadWidget**

  * Select/capture file → Upload to `/Media` with pointer to `owner`, `listing`
  * Return ParseFile URL
* **ImageGalleryViewer**

  * Display all images from `images` Relation in ProductListing

---

## 5. Authentication (Firebase Auth)

* **Login/SignupScreen**

  * Firebase Auth (Email/Password)
  * On success:

    * Get `firebaseUid`
    * POST to `/users` on Parse with `firebaseUid`
  * Store UID in local storage
* **SessionManager**

  * Use Firebase token for authenticated requests
  * Parse API: pass Firebase UID in custom header or use Cloud Code to validate token

---

## 6. Messaging (Firebase Realtime Database)

* **ChatListScreen**

  * GET `/conversations/{userId}`
  * Show last message preview, timestamp
* **MessageThreadScreen**

  * Sync with `/messages/{conversationId}`
  * List messages with timestamps
  * Input field: Send new message → POST to Firebase

---

## 7. Role-Based Behavior

* On login, fetch Parse user role:

  ```kotlin
  ParseUser.getCurrentUser().get("role")
  ```
* Adjust navigation and access:

  * If role == "Farmer": show FarmerNav
  * If role == "General": show GeneralNav

---

## 8. Future Extensibility (Prep for Breeders)

* Structure UI with modular roles in mind
* Add switch-case handlers for "HighLevel" role
* Prepare components: FarmDashboard, TraceTree, BreederAnalytics

---

## 9. UI Frameworks & Tools

* Use **Jetpack Compose** for dynamic UI
* Use **Coil** or **Glide** for media loading
* Use **Firebase SDK** for auth and chat
* Use **Retrofit** or **Ktor** for Parse API calls
* Use **StateFlow** + **ViewModel** for state management

---

By following this structure, each frontend component communicates seamlessly with the backend Parse and Firebase systems. This ensures clear integration paths for each screen and prepares the app for future feature upgrades like breeder traceability, advanced analytics, and farm dashboards.

