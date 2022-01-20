import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.Serializable
import java.lang.Exception
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.ArrayList
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties

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
        val data = doc.data
        val bundlesRefs = data!!["bundles"] as ArrayList<DocumentReference>

        // parse date
        val firebaseTimestamp = data["date"] as Timestamp
        val javaDate = firebaseTimestamp.toDate()
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault());

        val recurring = data["recurring"] as Boolean
        val daysToRepeat: Int
        daysToRepeat = if(recurring) (data["days_to_repeat"] as Long).toInt() else 0

        val userOrderTask = DatabaseItemTask()
        val bundles = mutableListOf<ShoppingBundle>()
        var remainingToFinish = bundlesRefs.size
        if (remainingToFinish == 0)
        {
            userOrderTask.finishTask(UserOrder(id, bundles,
                DatabaseField("date",date),
                DatabaseField("days_to_repeat",daysToRepeat),
                DatabaseField("recurring",recurring),
                doc.reference))
        }
        for(bundleRef in bundlesRefs)
        {
            bundleRef.get().addOnSuccessListener { bundleDoc->
                bundleFromDocument(bundleDoc).addOnSuccessListener { bundle->
                    bundles.add(bundle as ShoppingBundle)
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
    fun addNewFridgeItem(measuringUnit: String, product: Product, quantity: Int)
    {
        val data = hashMapOf(
            "measuring_unit" to measuringUnit,
            "product" to product.databaseRef,
            "quantity" to quantity
        )
        db.collection("UserData").document(auth.uid.toString()).collection("Fridge").add(data).addOnSuccessListener {
            val id = it.id
            FridgeItem(id,
                DatabaseField("measuring_unit",measuringUnit),
                product,
                DatabaseField("quantity",quantity),
                it)
        }
    }

    fun addNewBundle(name: String, itemsInBundle: List<BundleItem>)
    {
        val data = hashMapOf(
            "name" to name
        )
        db.collection("UserData").document(auth.uid.toString()).collection("Bundles").add(data).addOnSuccessListener {
            val id = it.id
            ShoppingBundle(id, DatabaseField("name", name), itemsInBundle, it)
        }
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

    class DatabaseField<T>(val dbName: String, dbVal: T)
    {
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
            onChangeListeners.add(listener)
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

        protected fun parseAndNotifyGroupListeners(eventType: DatabaseEventType)
        {
            val dataType: KType = this::class.createType()

            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(this,eventType)
                }
            }
        }

        protected fun notifyFieldListeners(fieldChanged: DatabaseField<Any>)
        {
            databaseRef.update(fieldChanged.dbName, fieldChanged.databaseValue).addOnSuccessListener {
                for(listener in onFieldChangeListeners)
                {
                    listener(fieldChanged)
                }
                parseAndNotifyGroupListeners(DatabaseEventType.MODIFIED)
            }.addOnFailureListener {
                    e -> Log.w(TAG, "Error updating document", e)
            }
        }
        fun addOnFieldChangeListener(listener:(DatabaseField<Any>) -> Unit)
        {
            onFieldChangeListeners.add(listener)
        }
        protected fun notifySubitemListeners(databaseItemChanged: DatabaseItem)
        {
            for(listener in onSubitemChangeListeners)
            {
                listener(databaseItemChanged)
            }
            parseAndNotifyGroupListeners(DatabaseEventType.MODIFIED)
        }
        fun addOnSubitemChangeListener(listener:(DatabaseItem) -> Unit)
        {
            onSubitemChangeListeners.add(listener)
        }
    }
    class Product(id: String, val name: DatabaseField<String>, val price: DatabaseField<Double>, val storeGivenId: DatabaseField<String>, val barcode: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        init {
            name.addOnChangeListener { notifyFieldListeners(name.eraseType()) }
            price.addOnChangeListener { notifyFieldListeners(price.eraseType()) }
            storeGivenId.addOnChangeListener { notifyFieldListeners(storeGivenId.eraseType()) }
            barcode.addOnChangeListener { notifyFieldListeners(barcode.eraseType()) }
            super.parseAndNotifyGroupListeners(DatabaseEventType.ADDED)
        }
    }
    abstract class QuantityItem(id: String, val measuringUnit: DatabaseField<String>, val product: Product, val quantity: DatabaseField<Int>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        init {
            measuringUnit.addOnChangeListener { notifyFieldListeners(measuringUnit.eraseType()) }
            quantity.addOnChangeListener { notifyFieldListeners(quantity.eraseType()) }
            measuringUnit.addOnChangeListener { notifyFieldListeners(measuringUnit.eraseType()) }
            product.addOnFieldChangeListener { notifySubitemListeners(product) }
            super.parseAndNotifyGroupListeners(DatabaseEventType.ADDED)
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
            super.parseAndNotifyGroupListeners(DatabaseEventType.ADDED)
        }
        fun addBundleItem(measuringUnit: String, product: Product, quantity: Int)
        {
            val data = hashMapOf(
                "measuring_unit" to measuringUnit,
                "quantity" to quantity
            )
            this.databaseRef.collection("Products").add(data).addOnSuccessListener {
                val id = it.id
                val newBundleItem = BundleItem(
                    id,
                    DatabaseField("measuring_unit",measuringUnit),
                    product,
                    DatabaseField("quantity",quantity),
                    it
                )
                items = items.plus(newBundleItem)
                newBundleItem.addOnFieldChangeListener { notifySubitemListeners(newBundleItem) }
            }

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
            super.parseAndNotifyGroupListeners(DatabaseEventType.ADDED)
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
            super.parseAndNotifyGroupListeners(DatabaseEventType.ADDED)
        }
        fun addUserOrder(newUserOrder: UserOrder)
        {
            userOrders = userOrders.plus(newUserOrder)
            newUserOrder.addOnFieldChangeListener { notifySubitemListeners(newUserOrder) }
            newUserOrder.addOnSubitemChangeListener { notifySubitemListeners(newUserOrder) }
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

