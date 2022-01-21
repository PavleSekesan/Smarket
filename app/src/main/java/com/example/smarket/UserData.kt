import android.app.Activity
import android.provider.ContactsContract
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Executor
import kotlin.reflect.KType
import kotlin.reflect.full.createType

object UserData {
    private val TAG = "UserData"

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth


    // Listeners called when an item is modified (added or removed)
    private val modifyListeners: MutableMap<KType, MutableList<(DatabaseItem?, DatabaseEventType) -> (Unit)>> = mutableMapOf()

    init {
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
                        Log.d(TAG, "Firebase listener $collection: ${doc.id}")
                        val newItemTask = when(collection) {
                            "Bundles" -> bundleFromDocument(doc)
                            "Fridge" -> fridgeItemFromDocument(doc)
                            else -> userOrderFromDocument(doc)
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
        return Product(id,
            DatabaseField("name",name),
            DatabaseField("price",price),
            DatabaseField("id", storeGivenId),
            DatabaseField("barcode", barcode),
            doc.reference)
    }

    private fun bundleItemFromDocument(doc: DocumentSnapshot): DatabaseItemTask {
        val productData = doc.data!!
        val itemId = doc.id
        val measuringUnit = productData["measuring_unit"] as String
        val quantity = (productData["quantity"] as Long).toInt()
        val productRef = productData["product"] as DocumentReference

        val newBundelItemTask = DatabaseItemTask()
        productRef.get().addOnSuccessListener { productDoc->
            val newBundleItem = BundleItem(
                itemId,
                DatabaseField("measuring_unit",measuringUnit),
                productFromDoc(productDoc),
                DatabaseField("quantity",quantity),
                doc.reference
            )
            newBundelItemTask.finishTask(newBundleItem)
        }

        //updateDatabaseMapThreadSafe(itemId, newBundleItem)
        return newBundelItemTask
    }

    private fun bundleFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val id = doc.id
        val data = doc.data
        val bundleName = data!!["name"].toString()

        val itemsInBundle = mutableListOf<BundleItem>()

        val bundleTask = DatabaseItemTask()
        Log.d("bundleFromDocument", "Started get for bundle ${id}")
        doc.reference.collection("Products").get().addOnSuccessListener { itemDocuments->
            Log.d(TAG, "$id: ${itemDocuments.count()}")
            var remainingToFinish = itemDocuments.count()
            if (remainingToFinish == 0)
            {
                bundleTask.finishTask(ShoppingBundle(id, DatabaseField("name",bundleName), itemsInBundle, doc.reference))
            }
            for(itemDoc in itemDocuments)
            {
                bundleItemFromDocument(itemDoc).addOnSuccessListener {
                    remainingToFinish--
                    itemsInBundle.add(it as BundleItem)
                    Log.d("bundleFromDocument", "Finished get for bunlde $id")
                    if(remainingToFinish == 0)
                    {
                        bundleTask.finishTask(ShoppingBundle(id, DatabaseField("name",bundleName), itemsInBundle, doc.reference))
                    }
                }
            }
        }
        return bundleTask
    }

    private fun userOrderFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val id = doc.id
        val data = doc.data ?: throw Exception("Error getting user order from document")

        // parse date
        val firebaseTimestamp = data["date"] as Timestamp
        val javaDate = firebaseTimestamp.toDate()
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault())

        val recurring = data["recurring"] as Boolean
        val daysToRepeat: Int
        daysToRepeat = if(recurring) (data["days_to_repeat"] as Long).toInt() else 0

        val userOrderTask = DatabaseItemTask()
        val bundles = mutableListOf<ShoppingBundle>()

        doc.reference.collection("Bundles").get().addOnSuccessListener { bundleDocs ->
            var remainingToFinish = bundleDocs.size()
            if (remainingToFinish == 0)
            {
                userOrderTask.finishTask(UserOrder(id, bundles,
                    DatabaseField("date",date),
                    DatabaseField("days_to_repeat",daysToRepeat),
                    DatabaseField("recurring",recurring),
                    doc.reference))
            }
            for(bundleDocWithRef in bundleDocs)
            {
                val bundleRef = bundleDocWithRef.data["bundle"] as DocumentReference
                bundleRef.get().addOnSuccessListener { bundleDoc ->
                    bundleFromDocument(bundleDoc).addOnSuccessListener { ret1->

                        val bundle = ret1 as ShoppingBundle
                        bundles.add(bundle)
                        remainingToFinish--
                        if(remainingToFinish == 0)
                        {
                            userOrderTask.finishTask(UserOrder(id, bundles,
                                DatabaseField("date",date),
                                DatabaseField("days_to_repeat",daysToRepeat),
                                DatabaseField("recurring",recurring),
                                doc.reference))
                        }
                    }
                }
            }
        }
        return userOrderTask
    }

    private fun fridgeItemFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val id = doc.id
        val data: Map<String, Any> = doc.data!!
        val measuringUnit = data["measuring_unit"] as String
        val ref = data["product"] as DocumentReference
        val quantity = data["quantity"] as Long
        val fridgeItemTask = DatabaseItemTask()
        ref.get().addOnSuccessListener { productDoc->
            val product = productFromDoc(productDoc)
            fridgeItemTask.finishTask(FridgeItem(id,
                DatabaseField("measuring_unit",measuringUnit),
                product,
                DatabaseField("quantity",quantity.toInt()),
                doc.reference))
        }
        return fridgeItemTask
    }

    private fun deliveryFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val id = doc.id
        val data = doc.data!!

        // parse date
        val firebaseTimestamp = data["date"] as Timestamp
        val javaDate = firebaseTimestamp.toDate()
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault());

        val status = data["status"] as String
        val deliveryTask = DatabaseItemTask()
        doc.reference.collection("UserOrders").get().addOnSuccessListener { userOrdersDocs->

            val userOrders = mutableListOf<UserOrder>()
            var remainingToFinish = userOrdersDocs.size()
            if (remainingToFinish == 0)
            {
                deliveryTask.finishTask(Delivery(id,
                    DatabaseField("date",date),
                    userOrders,
                    DatabaseField("status",status),
                    doc.reference))
            }
            for(userOrdersDoc in userOrdersDocs)
            {
                (userOrdersDoc.data["order"] as DocumentReference).get().addOnSuccessListener { userOrderDoc->
                    userOrderFromDocument(userOrderDoc).addOnSuccessListener { userOrder->
                        userOrders.add(userOrder as UserOrder)
                        remainingToFinish--
                        if (remainingToFinish == 0)
                        {
                            deliveryTask.finishTask(Delivery(id,
                                DatabaseField("date",date),
                                userOrders,
                                DatabaseField("status",status),
                                doc.reference))
                        }
                    }
                }
            }
        }
        return deliveryTask
    }
    //endregion

    //region Methods that get all objects of certain type from DB
    fun getAllBundles() : DatabaseItemListTask
    {
        val bundlesTask = DatabaseItemListTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Bundles").get().addOnSuccessListener { bundlesDocs->
            val allBundles = mutableListOf<ShoppingBundle>()
            var remainingToFinish = bundlesDocs.size()
            if(remainingToFinish == 0)
            {
                bundlesTask.finishTask(allBundles)
            }
            for (document in bundlesDocs)
            {
                bundleFromDocument(document).addOnSuccessListener { bundle->
                    remainingToFinish--
                    allBundles.add(bundle as ShoppingBundle)
                    if(remainingToFinish == 0)
                    {
                        bundlesTask.finishTask(allBundles)
                    }
                }
            }
        }
        return bundlesTask
    }

    fun getAllUserOrders(): DatabaseItemListTask
    {
        val userOrdersTask = DatabaseItemListTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Orders").get().addOnSuccessListener { documents->
            val allUserOrders: MutableList<UserOrder> = mutableListOf()
            var remainingToFinish = documents.size()
            if(remainingToFinish == 0)
            {
                userOrdersTask.finishTask(allUserOrders)
            }
            for (document in documents) {
                userOrderFromDocument(document).addOnSuccessListener { userOrder->
                    allUserOrders.add(userOrder as UserOrder)
                    remainingToFinish--
                    if(remainingToFinish == 0)
                    {
                        userOrdersTask.finishTask(allUserOrders)
                    }
                }
            }
        }
        return userOrdersTask
    }

    fun getAllFridgeItems() : DatabaseItemListTask
    {
        val fridgeItemsTask = DatabaseItemListTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Fridge").get().addOnSuccessListener { documents->
            val allFridgeItems = mutableListOf<FridgeItem>()
            var remainingToFinish = documents.size()
            if (remainingToFinish == 0)
            {
                fridgeItemsTask.finishTask(allFridgeItems)
            }
            for (document in documents)
            {
                fridgeItemFromDocument(document).addOnSuccessListener { fridgeItem->
                    allFridgeItems.add(fridgeItem as FridgeItem)
                    remainingToFinish--
                    if (remainingToFinish == 0)
                    {
                        fridgeItemsTask.finishTask(allFridgeItems)
                    }
                }
            }
        }
        return fridgeItemsTask
    }

    fun getAllDeliveries(): DatabaseItemListTask
    {
        val deliveriesTask = DatabaseItemListTask()
        db.collection("Orders").whereEqualTo("userId", auth.uid.toString()).get().addOnSuccessListener { documents->
            val allDeliveries: MutableList<Delivery> = mutableListOf()
            var remainingToFinish = documents.size()
            if(remainingToFinish == 0)
            {
                deliveriesTask.finishTask(allDeliveries)
            }
            for (document in documents) {
                deliveryFromDocument(document).addOnSuccessListener { delivery->
                    allDeliveries.add(delivery as Delivery)
                    remainingToFinish--
                    if(remainingToFinish == 0)
                    {
                        deliveriesTask.finishTask(allDeliveries)
                    }
                }
            }
        }
        return deliveriesTask
    }
    //endregion

    //region Methods that instantiate new database objects
    fun addNewFridgeItem(measuringUnit: String, product: Product, quantity: Int) : DatabaseItemTask
    {
        val data = hashMapOf(
            "measuring_unit" to measuringUnit,
            "product" to product.databaseRef,
            "quantity" to quantity
        )
        val fridgeItemTask = DatabaseItemTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Fridge").add(data).addOnSuccessListener {
            val id = it.id
            val newFridgeItem = FridgeItem(id,
                DatabaseField("measuring_unit",measuringUnit),
                product,
                DatabaseField("quantity",quantity),
                it)
            val dataType: KType = newFridgeItem::class.createType()
            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(newFridgeItem,DatabaseEventType.ADDED)
                }
            }
            fridgeItemTask.finishTask(newFridgeItem)
        }
        return fridgeItemTask
    }

    fun addNewBundle(name: String, itemsInBundle: List<BundleItem>) : DatabaseItemTask
    {
        val data = hashMapOf(
            "name" to name
        )
        val newBundleTask = DatabaseItemTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Bundles").add(data).addOnSuccessListener {
            val id = it.id
            newBundleTask.finishTask(ShoppingBundle(id, DatabaseField("name", name), itemsInBundle, it))
        }
        return newBundleTask
    }
    suspend fun suspendAddNewBundle(name: String, itemsInBundle: List<BundleItem>) : ShoppingBundle
    {
        val data = hashMapOf(
            "name" to name
        )
        val doc = db.collection("UserData").document(auth.uid.toString()).collection("Bundles").add(data).await()
        return ShoppingBundle(doc.id, DatabaseField("name", name), itemsInBundle, doc)
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

    fun addOnBundleItemModifyListener(listener: (BundleItem?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is BundleItem)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper, BundleItem::class.createType())
    }

    fun addOnShoppingBundleModifyListener(listener: (ShoppingBundle?, DatabaseEventType) -> Unit)
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

    fun addOnDeliveryModifyListener(listener: (Delivery?, DatabaseEventType) -> Unit)
    {
        val wrapper: (DatabaseItem?, DatabaseEventType) -> Unit = { databaseItem: DatabaseItem?, databaseEventType: DatabaseEventType ->
            if(databaseItem is Delivery)
            {
                listener(databaseItem, databaseEventType)
            }
        }
        addOnModifyDatabaseItemListener(wrapper, Delivery::class.createType())
    }


    //endregion

    enum class DatabaseEventType {
        MODIFIED,
        ADDED,
        REMOVED
    }

    class DatabaseField<T>(val dbName: String, private var dbVal: T)
    {
        enum class DatabaseFieldEventType
        {
            DOWNLOAD,
            UPLOAD
        }

        private val onChangeListeners: MutableList<(T, DatabaseFieldEventType) -> Unit> = mutableListOf()
        var databaseValue = dbVal
            set(value){
                field=value
                dbVal = value
                for(listener in onChangeListeners)
                {
                    listener(value, DatabaseFieldEventType.UPLOAD)
                }
            }
            get() = dbVal
        fun addOnChangeListener(listener: (T, DatabaseFieldEventType) -> Unit)
        {
            onChangeListeners.add(listener)
        }
        fun eraseType() : DatabaseField<Any>
        {
            return DatabaseField(dbName, this.databaseValue as Any)
        }
        fun bindToDatabaseListner(docReference: DocumentReference)
        {
            docReference.addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }
                var newDbValue = doc?.data?.get(dbName)

                if (newDbValue == null)
                {
                    return@addSnapshotListener
                }

                if (newDbValue is Timestamp)
                {
                    val javaDate = newDbValue.toDate()
                    newDbValue = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault())
                }
                else if(newDbValue is Number && databaseValue is Int)
                {
                    newDbValue = newDbValue.toInt()
                }
                else if(newDbValue is Number && databaseValue is Double)
                {
                    newDbValue = newDbValue.toDouble()
                }

                if(newDbValue != databaseValue)
                {
                    dbVal = newDbValue as T
                    for(listener in onChangeListeners)
                    {
                        listener(newDbValue, DatabaseFieldEventType.DOWNLOAD)
                    }
                }
            }
        }
    }

    abstract class DatabaseItem(val id: String, val databaseRef: DocumentReference)
    {
        private val onFieldChangeListeners: MutableList<(DatabaseField<Any>)->Unit> = mutableListOf()
        private val onSubitemChangeListeners: MutableList<(DatabaseItem, DatabaseEventType)->Unit> = mutableListOf()

        protected fun parseAndNotifyGroupListeners(eventType: DatabaseEventType)
        {
            val dataType: KType = this::class.createType()

            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(this,eventType)
                }
            }
        }

        protected fun notifyFieldListeners(fieldChanged: DatabaseField<Any>, eventType: DatabaseField.DatabaseFieldEventType)
        {
            if(eventType == DatabaseField.DatabaseFieldEventType.UPLOAD) {
                databaseRef.update(fieldChanged.dbName, fieldChanged.databaseValue)
                    .addOnSuccessListener {
                        for (listener in onFieldChangeListeners) {
                            listener(fieldChanged)
                        }
                        parseAndNotifyGroupListeners(DatabaseEventType.MODIFIED)
                    }.addOnFailureListener { e ->
                    Log.w(TAG, "Error updating document", e)
                }
            }
            else
            {
                for (listener in onFieldChangeListeners) {
                    listener(fieldChanged)
                }
            }
        }
        fun addOnFieldChangeListener(listener:(DatabaseField<Any>) -> Unit)
        {
            onFieldChangeListeners.add(listener)
        }
        protected fun notifySubitemListeners(databaseItemChanged: DatabaseItem, eventType: DatabaseEventType)
        {
            for(listener in onSubitemChangeListeners)
            {
                listener(databaseItemChanged, eventType)
            }
            parseAndNotifyGroupListeners(DatabaseEventType.MODIFIED)
        }
        fun addOnSubitemChangeListener(listener:(DatabaseItem, DatabaseEventType) -> Unit)
        {
            onSubitemChangeListeners.add(listener)
        }
    }

    class Product(id: String, val name: DatabaseField<String>, val price: DatabaseField<Double>, val storeGivenId: DatabaseField<String>, val barcode: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        init {
            name.addOnChangeListener { v, t -> notifyFieldListeners(name.eraseType(), t) }
            name.bindToDatabaseListner(databaseRef)
            price.addOnChangeListener { v, t -> notifyFieldListeners(price.eraseType(), t) }
            price.bindToDatabaseListner(databaseRef)
            storeGivenId.addOnChangeListener { v, t -> notifyFieldListeners(storeGivenId.eraseType(), t) }
            storeGivenId.bindToDatabaseListner(databaseRef)
            barcode.addOnChangeListener { v, t -> notifyFieldListeners(barcode.eraseType(), t) }
            barcode.bindToDatabaseListner(databaseRef)
        }
    }

    abstract class QuantityItem(id: String, val measuringUnit: DatabaseField<String>, val product: Product, val quantity: DatabaseField<Int>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        init {
            measuringUnit.addOnChangeListener {v, t -> notifyFieldListeners(measuringUnit.eraseType(),t) }
            measuringUnit.bindToDatabaseListner(databaseRef)
            quantity.addOnChangeListener {v, t -> notifyFieldListeners(quantity.eraseType(),t) }
            quantity.bindToDatabaseListner(databaseRef)
            product.addOnFieldChangeListener { notifySubitemListeners(product, DatabaseEventType.MODIFIED) }
        }
    }

    class FridgeItem(id: String, measuringUnit: DatabaseField<String>, product: Product, quantity: DatabaseField<Int>, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)

    class BundleItem(id: String, measuringUnit: DatabaseField<String>, product: Product, quantity: DatabaseField<Int>, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)

    class ShoppingBundle(id: String, val name: DatabaseField<String>, bundleItems: List<BundleItem>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        var items: List<BundleItem> = bundleItems
            private set
        init {
            name.addOnChangeListener { v, t -> notifyFieldListeners(name.eraseType(),t) }
            name.bindToDatabaseListner(databaseRef)
            for(item in items) {
                item.addOnFieldChangeListener { notifySubitemListeners(item, DatabaseEventType.MODIFIED) }
            }

            databaseRef.collection("Products").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    if(dc.type == DocumentChange.Type.ADDED)
                    {
                        if(items.none { item -> item.id == dc.document.id }) {
                            bundleItemFromDocument(dc.document).addOnSuccessListener {
                                val newBundleItem = it as BundleItem
                                items = items.plus(newBundleItem)
                                newBundleItem.addOnFieldChangeListener {
                                    notifySubitemListeners(newBundleItem, DatabaseEventType.MODIFIED)
                                }
                                notifySubitemListeners(newBundleItem, DatabaseEventType.ADDED)
                            }
                        }
                    }
                    else if(dc.type == DocumentChange.Type.REMOVED)
                    {
                        val bundleIndexToRemove = items.indexOfFirst { bundle -> bundle.id == dc.document.id }
                        if(bundleIndexToRemove != -1)
                        {
                            val bundleItemToRemove = items[bundleIndexToRemove]
                            items = items.filter { bundle -> bundle.id != dc.document.id }
                            notifySubitemListeners(bundleItemToRemove, DatabaseEventType.REMOVED)
                        }
                    }
                }
            }
        }
        fun addBundleItem(measuringUnit: String, product: Product, quantity: Int) : DatabaseItemTask
        {
            val data = hashMapOf(
                "measuring_unit" to measuringUnit,
                "quantity" to quantity,
                "product" to product.databaseRef
            )
            val bundleItemTask = DatabaseItemTask()
            this.databaseRef.collection("Products").add(data).addOnSuccessListener {
                val id = it.id
                val newBundleItem = BundleItem(
                    id,
                    DatabaseField("measuring_unit",measuringUnit),
                    product,
                    DatabaseField("quantity",quantity),
                    it
                )
                val itemAlreadyInserted = items.filter { item-> item.id == it.id }
                if(itemAlreadyInserted.isEmpty())
                {
                    items = items.plus(newBundleItem)
                    newBundleItem.addOnFieldChangeListener { notifySubitemListeners(newBundleItem,DatabaseEventType.MODIFIED) }
                    notifySubitemListeners(newBundleItem, DatabaseEventType.ADDED)
                    bundleItemTask.finishTask(newBundleItem)
                }
                else
                {
                    bundleItemTask.finishTask(itemAlreadyInserted[0])
                }

            }
            return bundleItemTask
        }
    }

    class UserOrder(id: String, shoppingBundles: List<ShoppingBundle>, val date: DatabaseField<LocalDateTime>, val daysToRepeat: DatabaseField<Int>, val recurring: DatabaseField<Boolean>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        var bundles: List<ShoppingBundle> = shoppingBundles
            private set
        init {
            date.addOnChangeListener {v, t -> notifyFieldListeners(date.eraseType(),t) }
            date.bindToDatabaseListner(databaseRef)
            daysToRepeat.addOnChangeListener {v, t -> notifyFieldListeners(daysToRepeat.eraseType(),t) }
            daysToRepeat.bindToDatabaseListner(databaseRef)
            recurring.addOnChangeListener {v, t -> notifyFieldListeners(recurring.eraseType(),t) }
            recurring.bindToDatabaseListner(databaseRef)
            for(bundle in bundles)
            {
                bundle.addOnFieldChangeListener { notifySubitemListeners(bundle, DatabaseEventType.MODIFIED) }
                bundle.addOnSubitemChangeListener { v,t -> notifySubitemListeners(bundle, DatabaseEventType.MODIFIED) }
            }

            databaseRef.collection("Bundles").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    if(dc.type == DocumentChange.Type.ADDED)
                    {
                        val bundleRef = dc.document.data["bundle"] as DocumentReference
                        bundleRef.get().addOnSuccessListener { bundleDoc->
                            bundleFromDocument(bundleDoc).addOnSuccessListener {
                                val newBundle = it as ShoppingBundle
                                if(bundles.none { item -> item.id == newBundle.id }) {
                                    bundles = bundles.plus(newBundle)
                                    newBundle.addOnFieldChangeListener {
                                        notifySubitemListeners(newBundle, DatabaseEventType.MODIFIED)
                                    }
                                    newBundle.addOnSubitemChangeListener { v,t->
                                        notifySubitemListeners(newBundle, DatabaseEventType.MODIFIED)
                                    }
                                    notifySubitemListeners(newBundle, DatabaseEventType.ADDED)
                                }
                            }
                        }
                    }
                    else if(dc.type == DocumentChange.Type.REMOVED)
                    {
                        val bundleIndexToRemove = bundles.indexOfFirst { bundle -> bundle.id == dc.document.id }
                        if (bundleIndexToRemove != -1)
                        {
                            val removedBundle = bundles[bundleIndexToRemove]
                            bundles = bundles.filter { bundle->bundle.id != removedBundle.id }
                            notifySubitemListeners(removedBundle, DatabaseEventType.REMOVED)
                        }

                    }
                }
            }
        }
        fun addBunlde(newBundle: ShoppingBundle) : DatabaseItemTask
        {
            val data = hashMapOf(
                "bundle" to newBundle.databaseRef
            )
            val bundleItemTask = DatabaseItemTask()
            this.databaseRef.collection("Bundles").document(newBundle.id).set(data).addOnSuccessListener {
                val itemAlreadyInserted = bundles.filter { item-> item.id == newBundle.id }
                if(itemAlreadyInserted.isEmpty())
                {
                    bundles = bundles.plus(newBundle)
                    newBundle.addOnFieldChangeListener { notifySubitemListeners(newBundle, DatabaseEventType.MODIFIED) }
                    newBundle.addOnSubitemChangeListener { v,t -> notifySubitemListeners(newBundle, DatabaseEventType.MODIFIED) }
                    notifySubitemListeners(newBundle, DatabaseEventType.ADDED)
                    bundleItemTask.finishTask(newBundle)
                }
                else
                {
                    bundleItemTask.finishTask(itemAlreadyInserted[0])
                }

            }
            return bundleItemTask
        }

        fun removeBundle(bundleToRemove: ShoppingBundle) : DatabaseItemTask
        {
            val bundleItemTask = DatabaseItemTask()
            if(bundles.any { bundle-> bundle.id == bundleToRemove.id })
            {
                this.databaseRef.collection("Bundles").document(bundleToRemove.id).delete().addOnSuccessListener {

                    val bundleIndexToRemove = bundles.indexOfFirst { bundle -> bundle.id == bundleToRemove.id }
                    if (bundleIndexToRemove != -1)
                    {
                        val removedBundle = bundles[bundleIndexToRemove]
                        bundles = bundles.filter { bundle->bundle.id != bundleToRemove.id }
                        notifySubitemListeners(removedBundle, DatabaseEventType.REMOVED)
                        bundleItemTask.finishTask(bundleToRemove)
                    }
                    else
                    {
                        bundleItemTask.finishTask(bundleToRemove)
                    }
                }
            }
            else
            {
                bundleItemTask.finishTask(bundleToRemove)
            }
            return bundleItemTask
        }
    }

    class Delivery(id: String, val date: DatabaseField<LocalDateTime>, userOrders: List<UserOrder>, val status: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        var userOrders: List<UserOrder> = userOrders
            private set
        init {
            date.addOnChangeListener {v, t -> notifyFieldListeners(date.eraseType(),t) }
            date.bindToDatabaseListner(databaseRef)
            status.addOnChangeListener {v, t -> notifyFieldListeners(status.eraseType(),t) }
            status.bindToDatabaseListner(databaseRef)
            for(userOrder in userOrders)
            {
                userOrder.addOnFieldChangeListener { notifySubitemListeners(userOrder, DatabaseEventType.MODIFIED) }
                userOrder.addOnSubitemChangeListener {v,t -> notifySubitemListeners(userOrder, DatabaseEventType.MODIFIED) }
            }
        }
    }

    class DatabaseItemTask: Task<DatabaseItem>()
    {
        private var completedTask = false
        private var successTask = false
        private lateinit var taskResult: DatabaseItem
        private val onSuccessListeners: MutableList<OnSuccessListener<in DatabaseItem>> = mutableListOf()
        override fun isComplete(): Boolean {
            return completedTask
        }

        override fun isSuccessful(): Boolean {
            return successTask
        }

        override fun isCanceled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getResult(): DatabaseItem {
            return taskResult
        }

        override fun <X : Throwable?> getResult(p0: Class<X>): DatabaseItem {
            TODO("Not yet implemented")
        }

        override fun getException(): Exception? {
            TODO("Not yet implemented")
        }

        override fun addOnSuccessListener(p0: OnSuccessListener<in DatabaseItem>): Task<DatabaseItem> {
            onSuccessListeners.add(p0)
            return this
        }

        override fun addOnSuccessListener(
            p0: Executor,
            p1: OnSuccessListener<in DatabaseItem>
        ): Task<DatabaseItem> {
            TODO("Not yet implemented")
        }

        override fun addOnSuccessListener(
            p0: Activity,
            p1: OnSuccessListener<in DatabaseItem>
        ): Task<DatabaseItem> {
            TODO("Not yet implemented")
        }

        override fun addOnFailureListener(p0: OnFailureListener): Task<DatabaseItem> {
            TODO("Not yet implemented")
        }

        override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<DatabaseItem> {
            TODO("Not yet implemented")
        }

        override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<DatabaseItem> {
            TODO("Not yet implemented")
        }

        private fun onTaskSuccess()
        {
            successTask = true
            for(listener in onSuccessListeners)
            {
                listener.onSuccess(taskResult)
            }
        }

        private fun onTaskCompleted()
        {
            completedTask = true
            // if successful
            onTaskSuccess()
        }

        fun finishTask(dbItem: DatabaseItem)
        {
            taskResult = dbItem
            onTaskCompleted()
        }

    }

    class DatabaseItemListTask: Task<List<DatabaseItem>>()
    {
        private var completedTask = false
        private var successTask = false
        private lateinit var taskResult: List<DatabaseItem>
        private val onSuccessListeners: MutableList<OnSuccessListener<in List<DatabaseItem>>> = mutableListOf()
        override fun isComplete(): Boolean {
            return completedTask
        }

        override fun isSuccessful(): Boolean {
            return successTask
        }

        override fun isCanceled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getResult(): List<DatabaseItem> {
            return taskResult
        }

        override fun <X : Throwable?> getResult(p0: Class<X>): List<DatabaseItem> {
            TODO("Not yet implemented")
        }

        override fun getException(): Exception? {
            TODO("Not yet implemented")
        }

        override fun addOnSuccessListener(p0: OnSuccessListener<in List<DatabaseItem>>): Task<List<DatabaseItem>> {
            onSuccessListeners.add(p0)
            return this
        }

        override fun addOnSuccessListener(
            p0: Executor,
            p1: OnSuccessListener<in List<DatabaseItem>>
        ): Task<List<DatabaseItem>> {
            TODO("Not yet implemented")
        }

        override fun addOnSuccessListener(
            p0: Activity,
            p1: OnSuccessListener<in List<DatabaseItem>>
        ): Task<List<DatabaseItem>> {
            TODO("Not yet implemented")
        }

        override fun addOnFailureListener(p0: OnFailureListener): Task<List<DatabaseItem>> {
            TODO("Not yet implemented")
        }

        override fun addOnFailureListener(
            p0: Executor,
            p1: OnFailureListener
        ): Task<List<DatabaseItem>> {
            TODO("Not yet implemented")
        }

        override fun addOnFailureListener(
            p0: Activity,
            p1: OnFailureListener
        ): Task<List<DatabaseItem>> {
            TODO("Not yet implemented")
        }

        private fun onTaskSuccess()
        {
            successTask = true
            for(listener in onSuccessListeners)
            {
                listener.onSuccess(taskResult)
            }
        }

        private fun onTaskCompleted()
        {
            completedTask = true
            // if successful
            onTaskSuccess()
        }

        fun finishTask(dbItem: List<DatabaseItem>)
        {
            taskResult = dbItem
            onTaskCompleted()
        }
    }
}

