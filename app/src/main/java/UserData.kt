import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.collections.ArrayList

object UserData {

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private lateinit var bundles: MutableList<Bundle>
    private lateinit var fridgeItems: MutableList<FridgeItem>
    private lateinit var userOrders: MutableList<UserOrder>
    private lateinit var orders: MutableList<Order>

    private suspend fun bundleFromDocument(doc: DocumentSnapshot): Bundle
    {
        val data = doc.data
        val bundleName = data!!["name"].toString()
        val productReferences = data["products"] as ArrayList<DocumentReference>

        val productsInBundle = mutableListOf<Product>()
        for(ref in productReferences)
        {
            val productData = ref.get().await().data
            val barcode = if(productData!!.containsKey("barcode")) productData["barcode"] as String else ""

            productsInBundle.add(Product(productData["name"] as String, productData["price"] as Double, productData["id"] as String, barcode))
        }
        return Bundle(bundleName, productsInBundle)
    }

    private suspend fun userOrderFromDocument(doc: DocumentSnapshot): UserOrder
    {
        val data = doc.data
        val bundlesRefs = data!!["bundles"] as ArrayList<DocumentReference>
        val date = data["date"] as Date
        val daysToRepeat = (data["days_to_repeat"] as Double).toInt()
        val recurring = data["recurring"] as Boolean

        val bundles = mutableListOf<Bundle>()
        for(bundleRef in bundlesRefs)
        {
            bundles.add(bundleFromDocument(bundleRef.get().await()))
        }
        return UserOrder(bundles, date, daysToRepeat, recurring)
    }

    suspend fun getAllBundles(): List<Bundle>
    {
        if (this::bundles.isInitialized)
        {
            return bundles
        }
        else
        {
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").get().await()
            val tempBundles = mutableListOf<Bundle>()
            for (document in documents)
            {
                tempBundles.add(bundleFromDocument(document))
            }

            bundles = tempBundles
            return bundles
        }
    }

    suspend fun getAllUserOrders(): List<UserOrder>
    {
        if(this::userOrders.isInitialized)
        {
            return userOrders
        }
        else
        {
            val tempOrders = mutableListOf<UserOrder>()
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Orders").get().await()
            for (document in documents) {
                tempOrders.add(userOrderFromDocument(document))
            }
            userOrders = tempOrders
            return userOrders
        }
    }

    suspend fun getAllFridgeItems(): List<FridgeItem>
    {
        if(this::fridgeItems.isInitialized)
        {
            return fridgeItems
        }
        else
        {
            val fridgeItemsUnit = mutableListOf<String>()
            val fridgeItemsIds = mutableListOf<String>()
            val fridgeItemsRef = mutableListOf<DocumentReference>()
            val fridgeItemQuantities = mutableListOf<Int>()
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").get().await()

            for (document in documents)
            {
                val id = document.id
                val data: Map<String, Any> = document.data
                val messuringUnit = data["measuring_unit"] as String
                val ref = data["product"] as DocumentReference
                val quantity = data["quantity"] as Double
                fridgeItemsIds.add(id)
                fridgeItemsUnit.add(messuringUnit)
                fridgeItemsRef.add(ref)
                fridgeItemQuantities.add(quantity.toInt())
            }

            val tempItems = mutableListOf<FridgeItem>()
            for(i in fridgeItemsUnit.indices)
            {
                val ref = fridgeItemsRef[i]
                val productData = ref.get().await().data
                val barcode = if(productData!!.containsKey("barcode")) productData["barcode"] as String else ""
                val product = Product(productData["name"] as String, productData["price"] as Double, productData["id"] as String, barcode)

                tempItems.add(FridgeItem(fridgeItemsIds[i],fridgeItemsUnit[i],product,fridgeItemQuantities[i]))
            }

            fridgeItems = tempItems
            return fridgeItems
        }
    }

    suspend fun getAllOrders(): List<Order>
    {
        if(this::orders.isInitialized)
        {
            return orders
        }
        else {
            orders = mutableListOf()
            val documents = db.collection("Orders").whereEqualTo("userId", auth.uid.toString()).get().await()
            for (document in documents) {
                val data = document.data
                val date = data["date"] as Date
                val status = data["status"] as String
                val ref = data["userOrder"] as DocumentReference
                val userOrder = userOrderFromDocument(ref.get().await())
                orders.add(Order(date,userOrder,status))
            }
            return orders
        }
    }

    fun updateFridgeQuantity(fridgeItem: FridgeItem, delta: Int): FridgeItem {
        val id = fridgeItem.id

        db.collection("UserData").document(auth.uid.toString()).collection("Fridge").document(id)
            .update("quantity", fridgeItem.quantity + delta)
        val newFridgeItem = FridgeItem(id,fridgeItem.mesuringUnit, fridgeItem.product,fridgeItem.quantity + delta)
        for(i in fridgeItems.indices)
        {
            if(fridgeItems[i].id == id)
            {
                fridgeItems[i] = newFridgeItem
            }
        }
        return newFridgeItem
    }

    suspend fun addItemToFridge(meassuringUnit: String, product: Product, quantity: Int) : FridgeItem
    {
        val data = hashMapOf(
            "measuring_unit" to meassuringUnit,
            "product" to db.collection("Products").document(product.id),
            "quantity" to quantity
        )
        val newDoc = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").add(data).await()
        return FridgeItem(newDoc.id, meassuringUnit,product,quantity)
    }

}

data class Product(val name: String, val price: Double, val id: String, val barcode: String)
data class FridgeItem(val id: String, val mesuringUnit: String, val product: Product, val quantity: Int)
data class Bundle(val name: String, val products: List<Product>)
data class UserOrder(val bundles: List<Bundle>, val date: Date, val daysToRepeat: Int, val recurring: Boolean)
data class Order(val date: Date, val userOrder: UserOrder, val status: String)
