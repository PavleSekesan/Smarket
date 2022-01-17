import android.util.Log
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.reflect

object UserData {
    private val TAG = "UserData"

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private lateinit var bundleIds: MutableList<String>
    private lateinit var fridgeItemsIds: MutableList<String>
    private lateinit var userOrdersIds: MutableList<String>
    private lateinit var ordersIds: MutableList<String>

    private val databaseItems = ObservableArrayMap<String, DatabaseItem>()

    // Listeners called when an item is modified (added or removed)
    private val modifyListeners: MutableMap<KType, MutableList<(DatabaseItem?, DatabaseEventType) -> (Unit)>> = mutableMapOf()

    init {
        databaseItems.addOnMapChangedCallback(ObservableMapListener())

        val collectionsToListen = listOf("Bundles", "Fridge", "Orders")
        for (collection in collectionsToListen)
        {
            db.collection("UserData").document(auth.uid.toString()).collection(collection)
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "Received $collection update from DB")
                    for (doc in value!!) {
                        GlobalScope.launch {
                            val newItem = when(collection) {
                                "Bundles" -> bundleFromDocument(doc)
                                "Fridge" -> fridgeItemFromDocument(doc)
                                else -> userOrderFromDocument(doc)
                            }
                            databaseItems[newItem.id] = newItem
                        }
                    }
                }
        }
    }

    //region Methods that get objects from the database
    private suspend fun productFromDoc(doc: DocumentSnapshot): Product
    {
        val id = doc.id
        val data = doc.data
        val storeGivenId = data!!["id"] as String
        val name = data["name"] as String
        val price = data["price"] as Double
        val barcode = if(data.containsKey("barcode")) data["barcode"] as String else ""
        return Product(id,name,price,storeGivenId,barcode)
    }

    private suspend fun bundleFromDocument(doc: DocumentSnapshot): ShoppingBundle
    {
        val id = doc.id
        val data = doc.data
        val bundleName = data!!["name"].toString()

        val itemsInBundle = mutableListOf<BundleItem>()
        val itemDocuments = doc.reference.collection("Products").get().await()
        Log.d(TAG, itemDocuments.count().toString())
        for(itemDoc in itemDocuments)
        {
            val productData = itemDoc.data
            val itemId = itemDoc.id
            val measuringUnit = productData["measuring_unit"] as String
            val quantity =  (productData["quantity"] as Long).toInt()
            val productRef = productData["product"] as DocumentReference
            val productDoc = productRef.get().await()
            itemsInBundle.add(BundleItem(itemId,measuringUnit,productFromDoc(productDoc),quantity))
        }

        return ShoppingBundle(id, bundleName, itemsInBundle)
    }

    private suspend fun userOrderFromDocument(doc: DocumentSnapshot): UserOrder
    {
        val id = doc.id
        val data = doc.data
        val bundlesRefs = data!!["bundles"] as ArrayList<DocumentReference>
        val date = data["date"] as Date
        val daysToRepeat = (data["days_to_repeat"] as Double).toInt()
        val recurring = data["recurring"] as Boolean

        val bundles = mutableListOf<ShoppingBundle>()
        for(bundleRef in bundlesRefs)
        {
            bundles.add(bundleFromDocument(bundleRef.get().await()))
        }
        return UserOrder(id, bundles, date, daysToRepeat, recurring)
    }

    private suspend fun fridgeItemFromDocument(doc: DocumentSnapshot): FridgeItem
    {
        val id = doc.id
        val data: Map<String, Any> = doc.data!!
        val measuringUnit = data["measuring_unit"] as String
        val ref = data["product"] as DocumentReference
        val quantity = data["quantity"] as Double
        val productDoc = ref.get().await()
        val product = productFromDoc(productDoc)
        return FridgeItem(id,measuringUnit,product,quantity.toInt())
    }

    private suspend fun deliveryFromDocument(doc: DocumentSnapshot): Delivery
    {
        val id = doc.id
        val data = doc.data!!
        val date = data["date"] as Date
        val status = data["status"] as String
        val ref = data["userOrder"] as DocumentReference
        val userOrder = userOrderFromDocument(ref.get().await())
        return Delivery(id,date,userOrder,status)
    }
    //endregion

    //region Methods that get all objects of certain type from DB
    suspend fun getAllBundles(): List<ShoppingBundle>
    {
        if (!this::bundleIds.isInitialized)
        {
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").get().await()
            bundleIds = mutableListOf()
            for (document in documents)
            {
                val bundle = bundleFromDocument(document)
                databaseItems[bundle.id] = bundle
                bundleIds.add(bundle.id)
            }
        }
        val bundles = mutableListOf<ShoppingBundle>()
        for(key in bundleIds)
        {
            bundles.add(databaseItems[key] as ShoppingBundle)
        }
        return bundles
    }

    suspend fun getAllUserOrders(): List<UserOrder>
    {
        if(!this::userOrdersIds.isInitialized)
        {
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Orders").get().await()
            userOrdersIds = mutableListOf()
            for (document in documents) {
                val userOrder = userOrderFromDocument(document)
                databaseItems[userOrder.id] = userOrder
                userOrdersIds.add(userOrder.id)
            }
        }
        val userOrders = mutableListOf<UserOrder>()
        for(key in userOrdersIds)
        {
            userOrders.add(databaseItems[key] as UserOrder)
        }
        return userOrders
    }

    suspend fun getAllFridgeItems(): List<FridgeItem>
    {
        if(!this::fridgeItemsIds.isInitialized)
        {
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").get().await()
            for (document in documents)
            {
                val item = fridgeItemFromDocument(document)
                databaseItems[item.id] = item
                fridgeItemsIds.add(item.id)
            }
        }
        val fridgeItems = mutableListOf<FridgeItem>()
        for(key in fridgeItemsIds)
        {
            fridgeItems.add(databaseItems[key] as FridgeItem)
        }
        return fridgeItems
    }

    suspend fun getAllDeliveries(): List<Delivery>
    {
        if(!this::ordersIds.isInitialized)
        {
            ordersIds = mutableListOf()
            val documents = db.collection("Orders").whereEqualTo("userId", auth.uid.toString()).get().await()
            for (document in documents) {
                val order = deliveryFromDocument(document)
                ordersIds.add(order.id)
                databaseItems[order.id] = order
            }
        }
        val orders = mutableListOf<Delivery>()
        for(key in ordersIds)
        {
            orders.add(databaseItems[key] as Delivery)
        }
        return orders
    }
    //endregion

    //region Methods that edit objects in the database
    suspend fun updateFridgeQuantity(fridgeItem: FridgeItem, delta: Int): FridgeItem {
        val id = fridgeItem.id

        db.collection("UserData").document(auth.uid.toString()).collection("Fridge").document(id)
            .update("quantity", fridgeItem.quantity + delta).await()
        val newFridgeItem = FridgeItem(id,fridgeItem.measuringUnit, fridgeItem.product,fridgeItem.quantity + delta)
        databaseItems[id] = newFridgeItem
        return newFridgeItem
    }

    suspend fun addItemToFridge(measuringUnit: String, product: Product, quantity: Int) : FridgeItem
    {
        val data = hashMapOf(
            "measuring_unit" to measuringUnit,
            "product" to db.collection("Products").document(product.id),
            "quantity" to quantity
        )
        val newDoc = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").add(data).await()
        val newFridgeItem = FridgeItem(newDoc.id, measuringUnit,product,quantity)
        databaseItems[newFridgeItem.id] = newFridgeItem
        return newFridgeItem
    }

    suspend fun addItemToBundle(bundle: ShoppingBundle, measuringUnit: String, product: Product, quantity: Int) : ShoppingBundle
    {
        val productRef = db.collection("Products").document(product.id)
        val data = hashMapOf(
            "measuring_unit" to measuringUnit,
            "product" to productRef,
            "quantity" to quantity
        )
        val id = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").document(bundle.id).collection("Products").add(data).await().id
        val newItem = BundleItem(id,measuringUnit,product,quantity)
        val newBundle = ShoppingBundle(bundle.id,bundle.name,bundle.items.plus(newItem))
        databaseItems[newBundle.id] = newBundle
        return newBundle
    }

    suspend fun addItemToBundle(bundleId: String, measuringUnit: String, product: Product, quantity: Int) : ShoppingBundle
    {
        val bundle = databaseItems[bundleId]!! as ShoppingBundle
        return addItemToBundle(bundle, measuringUnit, product, quantity)
    }

    suspend fun addNewBundle(name: String): ShoppingBundle
    {
        val data = hashMapOf(
            "name" to name
        )
        val id = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").add(data).await().id
        val newBundle = ShoppingBundle(id,name,emptyList())
        databaseItems[id] = newBundle
        return newBundle
    }
    //endregion

    //region Listeners and helper methods
    private fun addOnModifyDatabaseItemListener(listener: (DatabaseItem?, DatabaseEventType) -> Unit)
    {
        val itemType = listener.reflect()!!.parameters[0]::class.createType()
        if (modifyListeners.containsKey(itemType))
        {
            modifyListeners[itemType]!!.add(listener)
        }
        else
        {
            modifyListeners[itemType] = mutableListOf()
        }

    }

    fun addOnFridgeModifyListener(listener: (FridgeItem?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is FridgeItem)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper)
    }

    fun addOnBundleModifyListener(listener: (BundleItem?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is BundleItem)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper)
    }

    fun addOnUserOrderModifyListener(listener: (UserOrder?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is UserOrder)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper)
    }

    fun addOnOrderModifyListener(listener: (Delivery?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is Delivery)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper)
    }

    class ObservableMapListener: ObservableMap.OnMapChangedCallback<ObservableArrayMap<String,DatabaseItem>,String,DatabaseItem>()
    {
        private val copyMap = mutableMapOf<String,DatabaseItem>()
        override fun onMapChanged(sender: ObservableArrayMap<String, DatabaseItem>?, key: String?) {
            if(sender != null && key != null)
            {
                val dataType: KType
                val dbItem: DatabaseItem
                val eventType: DatabaseEventType
                if(sender.containsKey(key) && copyMap.containsKey(key)) // Item modified
                {
                    dataType = sender[key]!!::class.createType()
                    dbItem = sender[key]!!
                    eventType = DatabaseEventType.MODIFIED
                    copyMap[key] = dbItem
                }
                else if(sender.containsKey(key) && !copyMap.containsKey(key)) // Item added
                {
                    dataType = sender[key]!!::class.createType()
                    dbItem = sender[key]!!
                    eventType = DatabaseEventType.ADDED
                    copyMap[key] = dbItem
                }
                else // Item removed
                {
                    dataType = copyMap[key]!!::class.createType()
                    dbItem = copyMap[key]!!
                    eventType = DatabaseEventType.REMOVED
                    copyMap.remove(key)
                }
                if (modifyListeners.containsKey(dataType)) {
                    for (listener in modifyListeners[dataType]!!) {
                        listener(dbItem,eventType)
                    }
                }
            }
        }
    }
    //endregion
}

enum class DatabaseEventType {
    MODIFIED,
    ADDED,
    REMOVED
}
abstract class DatabaseItem(val id: String)
class Product(id: String, val name: String, val price: Double, val storeGivenId: String, val barcode: String) : DatabaseItem(id)
abstract class QuantityItem(id: String, val measuringUnit: String, val product: Product, val quantity: Int) : DatabaseItem(id)
class FridgeItem(id: String, measuringUnit: String, product: Product, quantity: Int) : QuantityItem(id, measuringUnit, product, quantity)
class BundleItem(id: String, measuringUnit: String, product: Product, quantity: Int) : QuantityItem(id, measuringUnit, product, quantity)
class ShoppingBundle(id: String, val name: String, val items: List<BundleItem>): DatabaseItem(id)
class UserOrder(id: String, val bundles: List<ShoppingBundle>, val date: Date, val daysToRepeat: Int, val recurring: Boolean): DatabaseItem(id)
class Delivery(id: String, val date: Date, val userOrder: UserOrder, val status: String): DatabaseItem(id)
