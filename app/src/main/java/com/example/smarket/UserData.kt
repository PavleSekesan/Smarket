import android.util.Log
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties

object UserData {
    private val TAG = "UserData"

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

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
                        }
                    }
                }
        }
    }

    //region Methods that get objects from the database
    fun updateFromDatabase(item: DatabaseItem)
    {
        item.databaseRef.get().addOnSuccessListener {
            val data = it.data!!
            val props = item::class.memberProperties
            for(prop in props)
            {
                val res = prop.getter.call()
                if(res is DatabaseField<*>)
                {
                    val fieldType = res.databaseValue
                    if (fieldType is Int)
                    {
                        val dbFieldCast = res as DatabaseField<Int>
                        dbFieldCast.databaseValue = data[dbFieldCast.dbName] as Int
                    }
                    else if(fieldType is String)
                    {
                        val dbFieldCast = res as DatabaseField<String>
                        dbFieldCast.databaseValue = data[dbFieldCast.dbName] as String
                    }
                    else if(fieldType is Boolean)
                    {
                        val dbFieldCast = res as DatabaseField<Boolean>
                        dbFieldCast.databaseValue = data[dbFieldCast.dbName] as Boolean
                    }
                    else if(fieldType is LocalDateTime)
                    {
                        val dbFieldCast = res as DatabaseField<LocalDateTime>
                        dbFieldCast.databaseValue = data[dbFieldCast.dbName] as LocalDateTime
                    }
                }
                else if(res is DatabaseItem)
                {

                }
                else if(res is List<*>)
                {

                }
                else
                {

                }
            }
        }
    }

    fun productFromDoc(doc: DocumentSnapshot): Product
    {
        val id = doc.id
        val data = doc.data
        val storeGivenId = data!!["id"] as String
        val name = data["name"] as String
        val price = data["price"] as Double
        val barcode = if(data.containsKey("barcode")) data["barcode"] as String else ""
        return Product(id,
            DatabaseField("name",name),
            DatabaseField("price",price),
            DatabaseField("id", storeGivenId),
            DatabaseField("barcode", barcode),
            doc.reference)
    }

    private suspend fun bundleItemFromDocument(doc: DocumentSnapshot): BundleItem {
        val productData = doc.data!!
        val itemId = doc.id
        val measuringUnit = productData["measuring_unit"] as String
        val quantity = (productData["quantity"] as Long).toInt()
        val productRef = productData["product"] as DocumentReference
        val productDoc = productRef.get().await()
        val newBundleItem = BundleItem(
            itemId,
            DatabaseField("measuring_unit",measuringUnit),
            productFromDoc(productDoc),
            DatabaseField("quantity",quantity),
            doc.reference
        )
        //updateDatabaseMapThreadSafe(itemId, newBundleItem)
        return newBundleItem
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
            itemsInBundle.add(bundleItemFromDocument(itemDoc))
        }

        return ShoppingBundle(id, DatabaseField("name",bundleName), itemsInBundle, doc.reference)
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
        return UserOrder(id, bundles,
            DatabaseField("date",date),
            DatabaseField("days_to_repeat",daysToRepeat),
            DatabaseField("recurring",recurring),
            doc.reference)
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
        return FridgeItem(id,
            DatabaseField("measuring_unit",measuringUnit),
            product,
            DatabaseField("quantity",quantity.toInt()),
            doc.reference)
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

        return Delivery(id,
            DatabaseField("date",date),
            userOrders,
            DatabaseField("status",status),
            doc.reference)
    }
    //endregion

    //region Methods that get all objects of certain type from DB
    fun getAllBundles()
    {
        val documents = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").get().addOnSuccessListener {
            for (document in it)
            {
                GlobalScope.launch {
                    bundleFromDocument(document)
                }
            }
        }
    }

    suspend fun getAllUserOrders(): List<UserOrder>
    {
        val documents = db.collection("UserData").document(auth.uid.toString()).collection("Orders").get().await()
        val allUserOrders: MutableList<UserOrder> = mutableListOf()
        for (document in documents) {
            val userOrder = userOrderFromDocument(document)
            allUserOrders.add(userOrder)
        }
        return allUserOrders
    }

    suspend fun getAllFridgeItems(): List<FridgeItem>
    {
        val documents = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").get().await()
        val allFridgeItems: MutableList<FridgeItem> = mutableListOf()
        for (document in documents)
        {
            val item = fridgeItemFromDocument(document)
            allFridgeItems.add(item)
        }
        return allFridgeItems
    }

    suspend fun getAllDeliveries(): List<Delivery>
    {
        val documents = db.collection("Orders").whereEqualTo("userId", auth.uid.toString()).get().await()
        val allDeliveries: MutableList<Delivery> = mutableListOf()
        for (document in documents) {
            val delivery = deliveryFromDocument(document)
            allDeliveries.add(delivery)
        }

        return allDeliveries
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

            }
        }
    }
    //endregion

    enum class DatabaseEventType {
        MODIFIED,
        ADDED,
        REMOVED
    }
    class DatabaseField<T>(val dbName: String, dbVal: T)
    {
        private val mutex = Mutex()
        private val onChangeListeners: MutableList<(T) -> Unit> = mutableListOf()
        var databaseValue = dbVal
            set(value){
                field=value
                for(listener in onChangeListeners)
                {
                    listener(value)
                }
            }
        fun addOnChangeListener(listener: (T) -> Unit)
        {
            GlobalScope.launch {
                mutex.withLock {
                    onChangeListeners.add(listener)
                }
            }
        }
        fun eraseType() : DatabaseField<Any>
        {
            return DatabaseField(dbName, this.databaseValue as Any)
        }
    }
    abstract class DatabaseItem(val id: String, val databaseRef: DocumentReference)
    {
        private val onFieldChangeListeners: MutableList<(DatabaseField<Any>)->Unit> = mutableListOf()
        private val onSubitemChangeListeners: MutableList<(DatabaseItem)->Unit> = mutableListOf()
        private val fieldChangeMutex = Mutex()
        private val subitemChangeMutex = Mutex()

        private fun parseAndNotifyGroupListeners()
        {
            val dataType: KType = this::class.createType()
            val eventType: DatabaseEventType = DatabaseEventType.MODIFIED

            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(this,eventType)
                }
            }
        }
        init {
            parseAndNotifyGroupListeners()
        }

        protected fun notifyFieldListeners(fieldChanged: DatabaseField<Any>)
        {
            databaseRef.update(fieldChanged.dbName, fieldChanged.databaseValue).addOnSuccessListener {
                for(listener in onFieldChangeListeners)
                {
                    listener(fieldChanged)
                }
                parseAndNotifyGroupListeners()
            }.addOnFailureListener {
                    e -> Log.w(TAG, "Error updating document", e)
            }
        }
        fun addOnFieldChangeListener(listener:(DatabaseField<Any>) -> Unit)
        {
            GlobalScope.launch {
                fieldChangeMutex.withLock {
                    onFieldChangeListeners.add(listener)
                }
            }
        }
        protected fun notifySubitemListeners(databaseItemChanged: DatabaseItem)
        {
            for(listener in onSubitemChangeListeners)
            {
                listener(databaseItemChanged)
            }
            parseAndNotifyGroupListeners()
        }
        fun addOnSubitemChangeListener(listener:(DatabaseItem) -> Unit)
        {
            GlobalScope.launch {
                subitemChangeMutex.withLock {
                    onSubitemChangeListeners.add(listener)
                }
            }
        }
    }
    class Product(id: String, val name: DatabaseField<String>, val price: DatabaseField<Double>, val storeGivenId: DatabaseField<String>, val barcode: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        init {
            name.addOnChangeListener { notifyFieldListeners(name.eraseType()) }
            price.addOnChangeListener { notifyFieldListeners(price.eraseType()) }
            storeGivenId.addOnChangeListener { notifyFieldListeners(storeGivenId.eraseType()) }
            barcode.addOnChangeListener { notifyFieldListeners(barcode.eraseType()) }
        }
    }
    abstract class QuantityItem(id: String, val measuringUnit: DatabaseField<String>, val product: Product, val quantity: DatabaseField<Int>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        init {
            measuringUnit.addOnChangeListener { notifyFieldListeners(measuringUnit.eraseType()) }
            quantity.addOnChangeListener { notifyFieldListeners(quantity.eraseType()) }
            measuringUnit.addOnChangeListener { notifyFieldListeners(measuringUnit.eraseType()) }
            product.addOnFieldChangeListener { notifySubitemListeners(product) }
        }
    }

    class FridgeItem(id: String, measuringUnit: DatabaseField<String>, product: Product, quantity: DatabaseField<Int>, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)
    class BundleItem(id: String, measuringUnit: DatabaseField<String>, product: Product, quantity: DatabaseField<Int>, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)

    class ShoppingBundle(id: String, val name: DatabaseField<String>, bundleItems: List<BundleItem>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        var items: List<BundleItem> = bundleItems
            private set
        init {
            name.addOnChangeListener { notifyFieldListeners(name.eraseType()) }
            for(item in items)
            {
                item.addOnFieldChangeListener { notifySubitemListeners(item) }
            }
        }
        fun addBundleItem(newBundleItem: BundleItem)
        {
            items = items.plus(newBundleItem)
            newBundleItem.addOnFieldChangeListener { notifySubitemListeners(newBundleItem) }
        }
    }
    class UserOrder(id: String, shoppingBundles: List<ShoppingBundle>, val date: DatabaseField<LocalDateTime>, val daysToRepeat: DatabaseField<Int>, val recurring: DatabaseField<Boolean>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        var bundles: List<ShoppingBundle> = shoppingBundles
            private set
        init {
            date.addOnChangeListener { notifyFieldListeners(date.eraseType()) }
            daysToRepeat.addOnChangeListener { notifyFieldListeners(daysToRepeat.eraseType()) }
            recurring.addOnChangeListener { notifyFieldListeners(recurring.eraseType()) }
            for(bundle in bundles)
            {
                bundle.addOnFieldChangeListener { notifySubitemListeners(bundle) }
                bundle.addOnSubitemChangeListener { notifySubitemListeners(bundle) }
            }
        }
        fun addBunlde(newBundle: ShoppingBundle)
        {
            bundles = bundles.plus(newBundle)
            newBundle.addOnFieldChangeListener { notifySubitemListeners(newBundle) }
            newBundle.addOnSubitemChangeListener { notifySubitemListeners(newBundle) }
        }
    }
    class Delivery(id: String, val date: DatabaseField<LocalDateTime>, userOrders: List<UserOrder>, val status: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        var userOrders: List<UserOrder> = userOrders
            private set
        init {
            date.addOnChangeListener { notifyFieldListeners(date.eraseType()) }
            status.addOnChangeListener { notifyFieldListeners(status.eraseType()) }
            for(userOrder in userOrders)
            {
                userOrder.addOnFieldChangeListener { notifySubitemListeners(userOrder) }
                userOrder.addOnSubitemChangeListener { notifySubitemListeners(userOrder) }
            }
        }
        fun addUserOrder(newUserOrder: UserOrder)
        {
            userOrders = userOrders.plus(newUserOrder)
            newUserOrder.addOnFieldChangeListener { notifySubitemListeners(newUserOrder) }
            newUserOrder.addOnSubitemChangeListener { notifySubitemListeners(newUserOrder) }
        }

    }
}

