import android.os.Bundle
import android.util.Log
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.reflect

object UserData {
    private val TAG = "UserData"

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private var bundlesInitialized = false
    private var fridgeItemsInitialized = false
    private var userOrdersInitialized = false
    private var deliveriesInitialized = false

    private val databaseItems = ObservableArrayMap<String, DatabaseItem>()
    private val databaseItemsMutex = Mutex()

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
                            updateDatabaseMapThreadSafe(newItem.id,newItem)
                        }
                    }
                }
        }
    }

    //region Methods that get objects from the database
    fun productFromDoc(doc: DocumentSnapshot): Product
    {
        val id = doc.id
        val data = doc.data
        val storeGivenId = data!!["id"] as String
        val name = data["name"] as String
        val price = data["price"] as Double
        val barcode = if(data.containsKey("barcode")) data["barcode"] as String else ""
        return Product(id,name,price,storeGivenId,barcode, doc.reference)
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
            itemsInBundle.add(BundleItem(itemId,measuringUnit,productFromDoc(productDoc),quantity, itemDoc.reference))
        }

        return ShoppingBundle(id, bundleName, itemsInBundle, doc.reference)
    }

    private suspend fun userOrderFromDocument(doc: DocumentSnapshot): UserOrder
    {
        val id = doc.id
        val data = doc.data
        val bundlesRefs = data!!["bundles"] as ArrayList<DocumentReference>

        // parse date
        val firebaseTimestamp = data["date"] as Timestamp
        val javaDate = firebaseTimestamp.toDate()
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault());

        val recurring = data["recurring"] as Boolean
        val daysToRepeat: Int
        daysToRepeat = if(recurring) (data["days_to_repeat"] as Long).toInt() else 0

        val bundles = mutableListOf<ShoppingBundle>()
        for(bundleRef in bundlesRefs)
        {
            bundles.add(bundleFromDocument(bundleRef.get().await()))
        }
        return UserOrder(id, bundles, date, daysToRepeat, recurring, doc.reference)
    }

    private suspend fun fridgeItemFromDocument(doc: DocumentSnapshot): FridgeItem
    {
        val id = doc.id
        val data: Map<String, Any> = doc.data!!
        val measuringUnit = data["measuring_unit"] as String
        val ref = data["product"] as DocumentReference
        val quantity = data["quantity"] as Long
        val productDoc = ref.get().await()
        val product = productFromDoc(productDoc)
        return FridgeItem(id,measuringUnit,product,quantity.toInt(),doc.reference)
    }

    private suspend fun deliveryFromDocument(doc: DocumentSnapshot): Delivery
    {
        val id = doc.id
        val data = doc.data!!

        // parse date
        val firebaseTimestamp = data["date"] as Timestamp
        val javaDate = firebaseTimestamp.toDate()
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault());

        val status = data["status"] as String
        val userOrdersDocs = doc.reference.collection("UserOrders").get().await()
        val userOrders = mutableListOf<UserOrder>()
        for(doc in userOrdersDocs)
        {
            val userOrderDoc = (doc.data["order"] as DocumentReference).get().await()
            userOrders.add(userOrderFromDocument(userOrderDoc))
        }

        return Delivery(id,date,userOrders,status,doc.reference)
    }
    //endregion

    //region Methods that get all objects of certain type from DB
    suspend fun getAllBundles(): List<ShoppingBundle>
    {
        if (!bundlesInitialized)
        {
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").get().await()
            bundlesInitialized = true
            for (document in documents)
            {
                val bundle = bundleFromDocument(document)
                updateDatabaseMapThreadSafe(bundle.id,bundle)
            }
        }
        return databaseItems.filter { kvp -> kvp.value is ShoppingBundle }.values.map { it as ShoppingBundle }
    }

    suspend fun getAllUserOrders(): List<UserOrder>
    {
        if(!userOrdersInitialized)
        {
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Orders").get().await()
            userOrdersInitialized = true
            for (document in documents) {
                val userOrder = userOrderFromDocument(document)
                updateDatabaseMapThreadSafe(userOrder.id,userOrder)
            }
        }
        return databaseItems.filter { kvp -> kvp.value is UserOrder }.values.map { it as UserOrder }
    }

    suspend fun getAllFridgeItems(): List<FridgeItem>
    {
        if(!fridgeItemsInitialized)
        {
            val documents = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").get().await()
            fridgeItemsInitialized = true
            for (document in documents)
            {
                val item = fridgeItemFromDocument(document)
                updateDatabaseMapThreadSafe(item.id,item)
            }
        }
        return databaseItems.filter { kvp -> kvp.value is FridgeItem }.values.map { it as FridgeItem }
    }

    suspend fun getAllDeliveries(): List<Delivery>
    {
        if(!deliveriesInitialized)
        {
            deliveriesInitialized = true
            val documents = db.collection("Orders").whereEqualTo("userId", auth.uid.toString()).get().await()
            for (document in documents) {
                val order = deliveryFromDocument(document)
                updateDatabaseMapThreadSafe(order.id,order)
            }
        }
        return databaseItems.filter { kvp -> kvp.value is Delivery }.values.map { it as Delivery }
    }
    //endregion

    //region Methods that edit objects in the database
    private suspend fun updateDatabaseMapThreadSafe(key: String, newValue: DatabaseItem)
    {
        databaseItemsMutex.withLock {
            databaseItems[key] = newValue
        }
    }

    suspend fun updateFridgeQuantity(fridgeItem: FridgeItem, delta: Int): FridgeItem {
        val id = fridgeItem.id

        db.collection("UserData").document(auth.uid.toString()).collection("Fridge").document(id)
            .update("quantity", fridgeItem.quantity + delta).await()
        val newFridgeItem = FridgeItem(id,fridgeItem.measuringUnit, fridgeItem.product,fridgeItem.quantity + delta, fridgeItem.databaseRef)
        updateDatabaseMapThreadSafe(id,newFridgeItem)
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
        val newFridgeItem = FridgeItem(newDoc.id, measuringUnit,product,quantity, newDoc)
        updateDatabaseMapThreadSafe(newFridgeItem.id,newFridgeItem)
        return newFridgeItem
    }

    suspend fun addItemToBundle(bundle: ShoppingBundle, measuringUnit: String, product: Product, quantity: Int) : BundleItem
    {
        val productRef = db.collection("Products").document(product.id)
        val data = hashMapOf(
            "measuring_unit" to measuringUnit,
            "product" to productRef,
            "quantity" to quantity
        )
        val newDoc = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").document(bundle.id).collection("Products").add(data).await()
        val newItem = BundleItem(newDoc.id,measuringUnit,product,quantity, newDoc)
        val newBundle = ShoppingBundle(bundle.id,bundle.name,bundle.items.plus(newItem),bundle.databaseRef)
        updateDatabaseMapThreadSafe(newBundle.id,newBundle)
        updateDatabaseMapThreadSafe(newItem.id,newItem)
        return newItem
    }

    suspend fun addItemToBundle(bundleId: String, measuringUnit: String, product: Product, quantity: Int) : BundleItem
    {
        val bundle = databaseItems[bundleId]!! as ShoppingBundle
        return addItemToBundle(bundle, measuringUnit, product, quantity)
    }

    suspend fun changeBundleName(bundle: ShoppingBundle, newName: String): ShoppingBundle
    {
        db.collection("UserData").document(auth.uid.toString()).collection("Bundles").document(bundle.id).update("name",newName).await()
        val newBundle = ShoppingBundle(bundle.id,newName,bundle.items,bundle.databaseRef)
        updateDatabaseMapThreadSafe(newBundle.id,newBundle)
        return newBundle
    }

    suspend fun changeBundleName(bundleId: String, newName: String): ShoppingBundle
    {
        val bundle = databaseItems[bundleId]!! as ShoppingBundle
        return changeBundleName(bundle, newName)
    }

    suspend fun updateBundleItemQuantity(bundleItem: BundleItem, delta: Int) : BundleItem
    {
        bundleItem.databaseRef.update("quantity",bundleItem.quantity+delta).await()
        return BundleItem(bundleItem.id,bundleItem.measuringUnit,bundleItem.product,bundleItem.quantity+delta,bundleItem.databaseRef)
    }

    suspend fun updateBundleItemQuantity(bundleItemId: String, delta: Int) : BundleItem
    {
        val bundleItem = databaseItems[bundleItemId]!! as BundleItem
        return updateBundleItemQuantity(bundleItem,delta)
    }

    suspend fun addNewBundle(name: String): ShoppingBundle
    {
        val data = hashMapOf(
            "name" to name
        )
        val doc = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").add(data).await()
        val newBundle = ShoppingBundle(doc.id,name,emptyList(),doc)
        updateDatabaseMapThreadSafe(doc.id,newBundle)
        return newBundle
    }
    //endregion

    //region Listeners and helper methods
    private fun addOnModifyDatabaseItemListener(listener: (DatabaseItem?, DatabaseEventType) -> Unit, itemType : KType)
    {
        if (modifyListeners.containsKey(itemType))
        {
            modifyListeners[itemType]!!.add(listener)
        }
        else
        {
            modifyListeners[itemType] = mutableListOf(listener)
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
        addOnModifyDatabaseItemListener(wrapper, FridgeItem::class.createType())
    }

    fun addOnBundleModifyListener(listener: (BundleItem?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is BundleItem)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper, BundleItem::class.createType())
    }

    fun addOnBundleModifyListener2(listener: (ShoppingBundle?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is ShoppingBundle)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper, ShoppingBundle::class.createType())
    }

    fun addOnUserOrderModifyListener(listener: (UserOrder?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is UserOrder)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper, UserOrder::class.createType())
    }

    fun addOnOrderModifyListener(listener: (Delivery?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is Delivery)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper, Delivery::class.createType())
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
abstract class DatabaseItem(val id: String, val databaseRef: DocumentReference)
class Product(id: String, val name: String, val price: Double, val storeGivenId: String, val barcode: String, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
abstract class QuantityItem(id: String, val measuringUnit: String, val product: Product, val quantity: Int, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
class FridgeItem(id: String, measuringUnit: String, product: Product, quantity: Int, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)
class BundleItem(id: String, measuringUnit: String, product: Product, quantity: Int, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)
class ShoppingBundle(id: String, val name: String, val items: List<BundleItem>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
class UserOrder(id: String, val bundles: List<ShoppingBundle>, val date: LocalDateTime, val daysToRepeat: Int, val recurring: Boolean, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
class Delivery(id: String, val date: LocalDateTime, val userOrders: List<UserOrder>, val status: String, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
