package com.example.mvp.data.models

import com.parse.ParseClassName
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseRelation
import com.parse.ParseRole
import com.parse.ParseUser
import org.json.JSONObject

/**
 * User model extending ParseUser with additional fields and methods.
 * Properties mirror the schema defined in the backend architecture.
 */
@ParseClassName("_User")
class User : ParseUser() {
    companion object {
        const val KEY_FIREBASE_UID = "firebaseUid"
        const val KEY_ROLE = "role"
        const val KEY_PROFILE_IMAGE = "profileImage"

        // Role names
        const val ROLE_FARMER = "Farmer"
        const val ROLE_GENERAL_USER = "GeneralUser"
        const val ROLE_ENTHUSIAST = "Enthusiast"
    }

    // Firebase UID
    var firebaseUid: String?
        get() = getString(KEY_FIREBASE_UID)
        set(value) = put(KEY_FIREBASE_UID, value ?: "")

    // Role (can be a pointer to ParseRole or String)
    var roleAsString: String?
        get() = getString(KEY_ROLE)
        set(value) = put(KEY_ROLE, value ?: "")

    var roleAsPointer: ParseRole?
        get() {
            val roleObject = getParseObject(KEY_ROLE)
            return if (roleObject is ParseRole) roleObject else null
        }
        set(value) = put(KEY_ROLE, value ?: JSONObject.NULL)

    // Profile image as a pointer to Media object
    var profileImage: Media?
        get() = getParseObject(KEY_PROFILE_IMAGE) as? Media
        set(value) = put(KEY_PROFILE_IMAGE, value ?: JSONObject.NULL)

    // Check if user is a farmer
    fun isFarmer(): Boolean {
        return roleAsString == ROLE_FARMER || (roleAsPointer?.name == ROLE_FARMER)
    }

    // Check if user is a general user
    fun isGeneralUser(): Boolean {
        return roleAsString == ROLE_GENERAL_USER || (roleAsPointer?.name == ROLE_GENERAL_USER)
    }
}

/**
 * Product listing model as defined in the schema.
 * Represents a product being sold in the marketplace.
 */
@ParseClassName("ProductListing")
class ProductListing : ParseObject() {
    companion object {
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_PRICE = "price"
        const val KEY_IS_TRACEABLE = "isTraceable"
        const val KEY_TRACE_ID = "traceId"
        const val KEY_SELLER = "seller"
        const val KEY_IMAGES = "images"

        // Query factory method
        fun getQuery(): ParseQuery<ProductListing> {
            return ParseQuery.getQuery(ProductListing::class.java)
        }
    }

    // Title
    var title: String?
        get() = getString(KEY_TITLE)
        set(value) = put(KEY_TITLE, value ?: "")

    // Description
    var description: String?
        get() = getString(KEY_DESCRIPTION)
        set(value) = put(KEY_DESCRIPTION, value ?: "")

    // Price
    var price: Number?
        get() = getNumber(KEY_PRICE)
        set(value) = put(KEY_PRICE, value ?: 0)

    // Is product traceable
    var isTraceable: Boolean
        get() = getBoolean(KEY_IS_TRACEABLE)
        set(value) = put(KEY_IS_TRACEABLE, value)

    // Trace ID for traceable products
    var traceId: String?
        get() = getString(KEY_TRACE_ID)
        set(value) = put(KEY_TRACE_ID, value ?: "")

    // Seller (User who posted this listing)
    var seller: User?
        get() = getParseUser(KEY_SELLER) as? User
        set(value) = put(KEY_SELLER, value ?: JSONObject.NULL)

    // Images relation - Parse supports both relation and array of pointers
    // This implementation uses relation
    fun getImagesRelation(): ParseRelation<Media> {
        return getRelation(KEY_IMAGES)
    }

    // Helper method to add an image to the relation
    fun addImage(media: Media) {
        getImagesRelation().add(media)
    }

    // Helper method to remove an image from the relation
    fun removeImage(media: Media) {
        getImagesRelation().remove(media)
    }
}

/**
 * Order model as defined in the schema.
 * Represents a transaction between buyer and seller.
 */
@ParseClassName("Order")
class Order : ParseObject() {
    companion object {
        const val KEY_BUYER = "buyer"
        const val KEY_SELLER = "seller"
        const val KEY_PRODUCT = "product"
        const val KEY_STATUS = "status"
        const val KEY_PRICE = "price"
        const val KEY_QUANTITY = "quantity"

        // Order status values
        const val STATUS_PENDING = "Pending"
        const val STATUS_CONFIRMED = "Confirmed"
        const val STATUS_SHIPPED = "Shipped"
        const val STATUS_DELIVERED = "Delivered"
        const val STATUS_COMPLETED = "Completed"
        const val STATUS_CANCELLED = "Cancelled"

        // Query factory method
        fun getQuery(): ParseQuery<Order> {
            return ParseQuery.getQuery(Order::class.java)
        }
    }

    // Buyer
    var buyer: User?
        get() = getParseUser(KEY_BUYER) as? User
        set(value) = put(KEY_BUYER, value ?: JSONObject.NULL)

    // Seller
    var seller: User?
        get() = getParseUser(KEY_SELLER) as? User
        set(value) = put(KEY_SELLER, value ?: JSONObject.NULL)

    // Product
    var product: ProductListing?
        get() = getParseObject(KEY_PRODUCT) as? ProductListing
        set(value) = put(KEY_PRODUCT, value ?: JSONObject.NULL)

    // Status
    var status: String?
        get() = getString(KEY_STATUS)
        set(value) = put(KEY_STATUS, value ?: STATUS_PENDING)

    // Final price
    var price: Number?
        get() = getNumber(KEY_PRICE)
        set(value) = put(KEY_PRICE, value ?: 0)

    // Quantity
    var quantity: Number?
        get() = getNumber(KEY_QUANTITY)
        set(value) = put(KEY_QUANTITY, value ?: 1)
}

/**
 * Media model as defined in the schema.
 * Represents images or videos for products or user profiles.
 */
@ParseClassName("Media")
class Media : ParseObject() {
    companion object {
        const val KEY_FILE = "file"
        const val KEY_OWNER = "owner"
        const val KEY_LISTING = "listing"
        const val KEY_CAPTION = "caption"
        const val KEY_MEDIA_TYPE = "mediaType"

        // Media types
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"

        // Query factory method
        fun getQuery(): ParseQuery<Media> {
            return ParseQuery.getQuery(Media::class.java)
        }
    }

    // The media file
    var file: ParseFile?
        get() = getParseFile(KEY_FILE)
        set(value) = put(KEY_FILE, value ?: JSONObject.NULL)

    // Owner/uploader
    var owner: User?
        get() = getParseUser(KEY_OWNER) as? User
        set(value) = put(KEY_OWNER, value ?: JSONObject.NULL)

    // Associated listing (optional)
    var listing: ProductListing?
        get() = getParseObject(KEY_LISTING) as? ProductListing
        set(value) = put(KEY_LISTING, value ?: JSONObject.NULL)

    // Caption
    var caption: String?
        get() = getString(KEY_CAPTION)
        set(value) = put(KEY_CAPTION, value ?: "")

    // Media type
    var mediaType: String?
        get() = getString(KEY_MEDIA_TYPE)
        set(value) = put(KEY_MEDIA_TYPE, value ?: TYPE_IMAGE)

    // Helper method to check if this is an image
    fun isImage(): Boolean {
        return mediaType == TYPE_IMAGE
    }

    // Helper method to check if this is a video
    fun isVideo(): Boolean {
        return mediaType == TYPE_VIDEO
    }
}

/**
 * User-to-user feedback model as defined in the schema.
 * Represents ratings and reviews between users.
 */
@ParseClassName("Feedback")
class Feedback : ParseObject() {
    companion object {
        const val KEY_FROM_USER = "fromUser"
        const val KEY_TO_USER = "toUser"
        const val KEY_RATING = "rating"
        const val KEY_COMMENT = "comment"
        const val KEY_ORDER = "order"

        // Query factory method
        fun getQuery(): ParseQuery<Feedback> {
            return ParseQuery.getQuery(Feedback::class.java)
        }
    }

    // User giving feedback
    var fromUser: User?
        get() = getParseUser(KEY_FROM_USER) as? User
        set(value) = put(KEY_FROM_USER, value ?: JSONObject.NULL)

    // User receiving feedback
    var toUser: User?
        get() = getParseUser(KEY_TO_USER) as? User
        set(value) = put(KEY_TO_USER, value ?: JSONObject.NULL)

    // Rating (1-5)
    var rating: Number?
        get() = getNumber(KEY_RATING)
        set(value) = put(KEY_RATING, value ?: 0)

    // Optional comment
    var comment: String?
        get() = getString(KEY_COMMENT)
        set(value) = put(KEY_COMMENT, value ?: "")

    // Related order
    var order: Order?
        get() = getParseObject(KEY_ORDER) as? Order
        set(value) = put(KEY_ORDER, value ?: JSONObject.NULL)
}

/**
 * Product feedback model as defined in the schema.
 * Represents ratings and reviews for products.
 */
@ParseClassName("ProductFeedback")
class ProductFeedback : ParseObject() {
    companion object {
        const val KEY_USER = "user"
        const val KEY_PRODUCT = "product"
        const val KEY_RATING = "rating"
        const val KEY_COMMENT = "comment"

        // Query factory method
        fun getQuery(): ParseQuery<ProductFeedback> {
            return ParseQuery.getQuery(ProductFeedback::class.java)
        }
    }

    // User giving feedback
    var user: User?
        get() = getParseUser(KEY_USER) as? User
        set(value) = put(KEY_USER, value ?: JSONObject.NULL)

    // Product being reviewed
    var product: ProductListing?
        get() = getParseObject(KEY_PRODUCT) as? ProductListing
        set(value) = put(KEY_PRODUCT, value ?: JSONObject.NULL)

    // Rating (1-5)
    var rating: Number?
        get() = getNumber(KEY_RATING)
        set(value) = put(KEY_RATING, value ?: 0)

    // Comment
    var comment: String?
        get() = getString(KEY_COMMENT)
        set(value) = put(KEY_COMMENT, value ?: "")
}

/**
 * Post model as defined in the schema.
 * Represents content shared by users in the community feed.
 */
@ParseClassName("Post")
class Post : ParseObject() {
    companion object {
        const val KEY_AUTHOR = "author"
        const val KEY_CONTENT = "content"
        const val KEY_MEDIA = "media"
        const val KEY_LIKES_COUNT = "likesCount"
        const val KEY_COMMENTS_COUNT = "commentsCount"
        const val KEY_TAGS = "tags"
        const val KEY_TITLE = "title"
        const val KEY_IS_FEATURED = "isFeatured"

        // Query factory method
        fun getQuery(): ParseQuery<Post> {
            return ParseQuery.getQuery(Post::class.java)
        }
    }

    // Post author
    var author: User?
        get() = getParseUser(KEY_AUTHOR) as? User
        set(value) = put(KEY_AUTHOR, value ?: JSONObject.NULL)

    // Post content/body
    var content: String?
        get() = getString(KEY_CONTENT)
        set(value) = put(KEY_CONTENT, value ?: "")

    // Post title (optional)
    var title: String?
        get() = getString(KEY_TITLE)
        set(value) = put(KEY_TITLE, value ?: "")

    // Media attachments - relation to Media objects
    fun getMediaRelation(): ParseRelation<Media> {
        return getRelation(KEY_MEDIA)
    }

    // Add media to post
    fun addMedia(media: Media) {
        getMediaRelation().add(media)
    }

    // Remove media from post
    fun removeMedia(media: Media) {
        getMediaRelation().remove(media)
    }

    // Likes count
    var likesCount: Int
        get() = getInt(KEY_LIKES_COUNT)
        set(value) = put(KEY_LIKES_COUNT, value)

    // Comments count
    var commentsCount: Int
        get() = getInt(KEY_COMMENTS_COUNT)
        set(value) = put(KEY_COMMENTS_COUNT, value)

    // Tags as a list of strings
    var tags: List<String>
        get() = getList<String>(KEY_TAGS) ?: emptyList()
        set(value) = put(KEY_TAGS, value)

    // Is featured post
    var isFeatured: Boolean
        get() = getBoolean(KEY_IS_FEATURED)
        set(value) = put(KEY_IS_FEATURED, value)
}

/**
 * Comment model for social interactions.
 * Represents comments on posts in the community feed.
 */
@ParseClassName("Comment")
class Comment : ParseObject() {
    companion object {
        const val KEY_POST = "post"
        const val KEY_AUTHOR = "author"
        const val KEY_CONTENT = "content"
        const val KEY_LIKES_COUNT = "likesCount"

        // Query factory method
        fun getQuery(): ParseQuery<Comment> {
            return ParseQuery.getQuery(Comment::class.java)
        }
    }

    // Associated post
    var post: Post?
        get() = getParseObject(KEY_POST) as? Post
        set(value) = put(KEY_POST, value ?: JSONObject.NULL)

    // Comment author
    var author: User?
        get() = getParseUser(KEY_AUTHOR) as? User
        set(value) = put(KEY_AUTHOR, value ?: JSONObject.NULL)

    // Comment text
    var content: String?
        get() = getString(KEY_CONTENT)
        set(value) = put(KEY_CONTENT, value ?: "")

    // Likes count
    var likesCount: Int
        get() = getInt(KEY_LIKES_COUNT)
        set(value) = put(KEY_LIKES_COUNT, value)
}