import android.app.Activity
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.lang.Exception
import java.time.*
import java.util.*
import java.util.concurrent.Executor
import kotlin.reflect.KType
import kotlin.reflect.full.createType

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.preference.PreferenceManager
import com.algolia.search.client.ClientSearch
import com.algolia.search.client.Index
import com.algolia.search.dsl.attributesToRetrieve
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.google.firebase.firestore.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


object UserData {
    private val TAG = "UserData"

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private val algoliaProductsIndex: Index


    // Listeners called when an item is modified (added or removed)
    private val modifyListeners: MutableMap<KType, MutableList<(DatabaseItem?, DatabaseEventType) -> (Unit)>> = mutableMapOf()

    init {
        val client = ClientSearch(
            applicationID = ApplicationID("HNV15SZ7ZV"),
            apiKey = APIKey("885cf7402b268ce00fcdc53a74232a37")
        )
        val indexName = IndexName("ProductName")
        algoliaProductsIndex = client.initIndex(indexName)


        val collectionsToListen = listOf("Bundles", "Fridge", "Orders")
        for (collection in collectionsToListen)
        {
            db.collection("UserData").document(auth.uid.toString()).collection(collection)
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

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
        db.collection("Deliveries").whereEqualTo("userId", auth.uid.toString())
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (dc in value!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d(TAG, "Received new delivery from DB")
                            val newItemTask = deliveryFromDocument(dc.document)
                            newItemTask.addOnSuccessListener {
                                val newDelivery = it as Delivery
                                val dataType: KType = newDelivery::class.createType()
                                if (modifyListeners.containsKey(dataType)) {
                                    for (listener in modifyListeners[dataType]!!) {
                                        listener(newDelivery,DatabaseEventType.ADDED)
                                    }
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            Log.d(TAG, "Delivery modified from DB")
                        }
                        DocumentChange.Type.REMOVED -> {
                            Log.d(TAG, "Delivery removed from DB")
                        }
                    }
                }

            }
    }

    //region Methods that get objects from the database
    fun productFromDoc(doc: DocumentSnapshot): Product?
    {
        if (!documentSafeToParse(Product::class.createType(),doc))
        {
            return null
        }
        val id = doc.id
        val data = doc.data
        val storeGivenId = data!!["id"] as String
        val name = data["name"] as String
        val price = (data["price"] as Number).toDouble()
        val barcode = if(data.containsKey("barcode")) data["barcode"] as String else ""
        return Product(id,
            DatabaseField("name",name),
            DatabaseField("price",price),
            DatabaseField("id", storeGivenId),
            DatabaseField("barcode", barcode),
            doc.reference)
    }

    private fun bundleItemFromDocument(doc: DocumentSnapshot): DatabaseItemTask {

        val newBundleItemTask = DatabaseItemTask()
        if (!documentSafeToParse(BundleItem::class.createType(),doc))
        {
            newBundleItemTask.finishTask(Exception("Document ${doc.id} does not contain all required fields"))
            return newBundleItemTask
        }
        val productData = doc.data!!
        val itemId = doc.id
        val measuringUnit = productData["measuring_unit"] as String
        val quantity = (productData["quantity"] as Long).toInt()
        val productRef = productData["product"] as DocumentReference

        productRef.get().addOnSuccessListener { productDoc->
            val product = productFromDoc(productDoc)
            if (product == null)
            {
                newBundleItemTask.finishTask(Exception("Product returned null in bundleItemFromDocument for document ${doc.id}"))
            }
            else {
                val newBundleItem = BundleItem(
                    itemId,
                    DatabaseField("measuring_unit", measuringUnit),
                    product,
                    DatabaseField("quantity", quantity),
                    doc.reference
                )
                newBundleItemTask.finishTask(newBundleItem)
            }
        }

        //updateDatabaseMapThreadSafe(itemId, newBundleItem)
        return newBundleItemTask
    }

    private fun bundleFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val bundleTask = DatabaseItemTask()
        if (!documentSafeToParse(ShoppingBundle::class.createType(),doc))
        {
            bundleTask.finishTask(Exception("Document ${doc.id} does not contain all required fields"))
            return bundleTask
        }
        val id = doc.id
        val data = doc.data
        val bundleName = data!!["name"].toString()

        val itemsInBundle = mutableListOf<BundleItem>()

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

    fun bundleFromId(bundleId: String) : DatabaseItemTask
    {
        val bundleTask = DatabaseItemTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Bundles").document(bundleId).get()
            .addOnSuccessListener { doc->
                bundleFromDocument(doc).addOnSuccessListener {
                    bundleTask.finishTask(it)
                }.addOnFailureListener {
                    bundleTask.finishTask(it)
                }
            }
            .addOnFailureListener {
                bundleTask.finishTask(Exception("Bundle with given id doesn't exist"))
            }
        return bundleTask
    }

    private fun userOrderFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val userOrderTask = DatabaseItemTask()
        if (!documentSafeToParse(UserOrder::class.createType(),doc))
        {
            userOrderTask.finishTask(Exception("Document ${doc.id} does not contain all required fields"))
            return userOrderTask
        }
        val id = doc.id
        val data = doc.data ?: throw Exception("Error getting user order from document")

        // parse date
        val firebaseTimestamp = data["date"] as Timestamp
        val javaDate = firebaseTimestamp.toDate()
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault())

        val recurring = data["recurring"] as Boolean
        val daysToRepeat: Int = if(recurring) (data["days_to_repeat"] as Number).toInt() else 0
        val status = data["status"] as String

        val bundles = mutableListOf<ShoppingBundle>()

        doc.reference.collection("Bundles").get().addOnSuccessListener { bundleDocs ->
            var remainingToFinish = bundleDocs.size()
            if (remainingToFinish == 0)
            {
                userOrderTask.finishTask(UserOrder(id, bundles,
                    DatabaseField("date",date),
                    DatabaseField("days_to_repeat",daysToRepeat),
                    DatabaseField("recurring",recurring),
                    DatabaseField("status",status),
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
                                DatabaseField("status",status),
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
        val fridgeItemTask = DatabaseItemTask()
        if (!documentSafeToParse(FridgeItem::class.createType(),doc))
        {
            fridgeItemTask.finishTask(Exception("Document ${doc.id} does not contain all required fields"))
            return fridgeItemTask
        }

        val id = doc.id
        if(doc.data != null) {
            val data: Map<String, Any> = doc.data!!
            val measuringUnit = data["measuring_unit"] as String
            val ref = data["product"] as DocumentReference
            val quantity = data["quantity"] as Long

            ref.get().addOnSuccessListener { productDoc ->
                val product = productFromDoc(productDoc)
                if (product == null)
                {
                    fridgeItemTask.finishTask(Exception("Product returned null in fridgeItemFromDocument ${doc.id}"))
                }
                else {
                    fridgeItemTask.finishTask(
                        FridgeItem(
                            id,
                            DatabaseField("measuring_unit", measuringUnit),
                            product,
                            DatabaseField("quantity", quantity.toInt()),
                            doc.reference
                        )
                    )
                }
            }
        }
        else
        {
            fridgeItemTask.finishTask(Exception("Document data is null"))
        }
        return fridgeItemTask
    }

    fun fridgeItemFromProduct(product: Product) : DatabaseItemTask
    {
        val fridgeItemTask = DatabaseItemTask()
        val id = product.id
        db.collection("UserData").document(auth.uid.toString()).collection("Fridge").document(id).get().addOnSuccessListener { doc->
            fridgeItemFromDocument(doc).addOnSuccessListener { ret1->
                fridgeItemTask.finishTask(ret1 as FridgeItem)
            }.addOnFailureListener { ex->
                fridgeItemTask.finishTask(ex)
            }
        }.addOnFailureListener { ex->
            fridgeItemTask.finishTask(ex)
        }
        return fridgeItemTask
    }

    private fun deliveryItemFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val deliveryItemTask = DatabaseItemTask()
        if (!documentSafeToParse(DeliveryItem::class.createType(),doc))
        {
            deliveryItemTask.finishTask(Exception("Document ${doc.id} does not contain all required fields"))
            return deliveryItemTask
        }

        val id = doc.id
        if(doc.data != null) {
            val data: Map<String, Any> = doc.data!!
            val measuringUnit = data["measuring_unit"] as String
            val ref = data["product"] as DocumentReference
            val quantity = (data["quantity"] as Number).toLong()

            ref.get().addOnSuccessListener { productDoc ->
                val product = productFromDoc(productDoc)
                if (product == null)
                {
                    deliveryItemTask.finishTask(Exception("Product returned null in fridgeItemFromDocument ${doc.id}"))
                }
                else {
                    deliveryItemTask.finishTask(
                        DeliveryItem(
                            id,
                            DatabaseField("measuring_unit", measuringUnit),
                            product,
                            DatabaseField("quantity", quantity.toInt()),
                            doc.reference
                        )
                    )
                }
            }
        }
        else
        {
            deliveryItemTask.finishTask(Exception("Document data is null"))
        }
        return deliveryItemTask
    }

    private fun deliveryFromDocument(doc: DocumentSnapshot): DatabaseItemTask
    {
        val deliveryTask = DatabaseItemTask()
        if (!documentSafeToParse(Delivery::class.createType(),doc))
        {
            deliveryTask.finishTask(Exception("Document ${doc.id} does not contain all required fields"))
            return deliveryTask
        }
        val id = doc.id
        val data = doc.data!!

        // parse date
        var firebaseTimestamp = data["date"] as Timestamp
        var javaDate = firebaseTimestamp.toDate()
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault())

        // parse end date
        firebaseTimestamp = data["end_date"] as Timestamp
        javaDate = firebaseTimestamp.toDate()
        val endDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(javaDate.time), ZoneId.systemDefault())

        val status = data["status"] as String

        val userOrders = mutableListOf<UserOrder>()
        val deliveryItems = mutableListOf<DeliveryItem>()
        var remainingUserOrdersToFinish = -1
        var remainingDeliveryItemsToFinish = -1

        doc.reference.collection("UserOrders").get().addOnSuccessListener { userOrdersDocs->
            remainingUserOrdersToFinish = userOrdersDocs.size()
            if (remainingUserOrdersToFinish == 0 && remainingDeliveryItemsToFinish == 0)
            {
                deliveryTask.finishTask(Delivery(id,
                    DatabaseField("date",date),
                    DatabaseField("end_date", endDate),
                    userOrders,
                    deliveryItems,
                    DatabaseField("status",status),
                    doc.reference))
            }
            for(userOrdersDoc in userOrdersDocs)
            {
                if (!userOrdersDoc.data.containsKey("order")) {
                    Log.e(TAG, "Failed to parse user order doc ${userOrdersDoc.id}")
                    continue
                }
                (userOrdersDoc.data["order"] as DocumentReference).get().addOnSuccessListener { userOrderDoc->
                    userOrderFromDocument(userOrderDoc).addOnSuccessListener { userOrder->
                        userOrders.add(userOrder as UserOrder)
                        remainingUserOrdersToFinish--
                        if (remainingUserOrdersToFinish == 0 && remainingDeliveryItemsToFinish == 0)
                        {
                            deliveryTask.finishTask(Delivery(id,
                                DatabaseField("date",date),
                                DatabaseField("end_date", endDate),
                                userOrders,
                                deliveryItems,
                                DatabaseField("status",status),
                                doc.reference))
                        }
                    }
                }
            }
        }

        doc.reference.collection("DeliveryItems").get().addOnSuccessListener { delievryItemsDocs ->
            remainingDeliveryItemsToFinish = delievryItemsDocs.size()
            if (remainingUserOrdersToFinish == 0 && remainingDeliveryItemsToFinish == 0)
            {
                deliveryTask.finishTask(Delivery(id,
                    DatabaseField("date",date),
                    DatabaseField("end_date", endDate),
                    userOrders,
                    deliveryItems,
                    DatabaseField("status",status),
                    doc.reference))
            }
            for(deliveryItemDoc in delievryItemsDocs)
            {
                deliveryItemFromDocument(deliveryItemDoc).addOnSuccessListener { deliveryItem->
                    deliveryItems.add(deliveryItem as DeliveryItem)
                    remainingDeliveryItemsToFinish--
                    if (remainingUserOrdersToFinish == 0 && remainingDeliveryItemsToFinish == 0)
                    {
                        deliveryTask.finishTask(Delivery(id,
                            DatabaseField("date",date),
                            DatabaseField("end_date", endDate),
                            userOrders,
                            deliveryItems,
                            DatabaseField("status",status),
                            doc.reference))
                    }
                }
            }
        }

        return deliveryTask
    }

    fun getAlgoliaProductSearch(textToSearch: String) : DatabaseItemListTask
    {
        val productsTask = DatabaseItemListTask()
        val searchQuery = com.algolia.search.dsl.query {
            query = textToSearch
            hitsPerPage = 10
            attributesToRetrieve {
                +"name"
                +"id"
            }
        }
        GlobalScope.launch {
            val result = algoliaProductsIndex.search(searchQuery)
            val products = mutableListOf<Product>()
            for(hit in result.hits)
            {
                val docId = hit["objectID"].toString().replace("\"","")
                val productName = hit["name"].toString().replace("\"","")
                val dbRef = db.collection("Products").document(docId)
                Log.d(TAG, "Search returned doc id $docId")

                products.add(Product(docId,
                    DatabaseField("name",productName),
                    DatabaseField("price",0.0),
                    DatabaseField("id", "0"),
                    DatabaseField("barcode", "0"),
                    dbRef))
            }

            Handler(Looper.getMainLooper()).post {
                productsTask.finishTask(products)
            }
        }
        return productsTask
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
        db.collection("Deliveries").whereEqualTo("userId", auth.uid.toString()).get().addOnSuccessListener { documents->
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
        if (quantity <= 0)
        {
            fridgeItemTask.finishTask(Exception("Fridge item quantity can't be <= 0"))
            return fridgeItemTask
        }
        val newDocReference = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").document(product.id)
        newDocReference.set(data).addOnSuccessListener {
            val newFridgeItem = FridgeItem(product.id,
                DatabaseField("measuring_unit",measuringUnit),
                product,
                DatabaseField("quantity",quantity),
                newDocReference)
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

    fun removeFridgeItemByProduct(product: Product) : DatabaseItemTask
    {
        val removeFridgeItemTask = DatabaseItemTask()
        val docRef = db.collection("UserData").document(auth.uid.toString()).collection("Fridge").document(product.id)
        docRef.get().addOnSuccessListener {
            fridgeItemFromDocument(it).addOnSuccessListener { dbItem->
                docRef.delete().addOnSuccessListener {
                    val fridgeItem = dbItem as FridgeItem
                    val dataType: KType = fridgeItem::class.createType()
                    if (modifyListeners.containsKey(dataType)) {
                        for (listener in modifyListeners[dataType]!!) {
                            listener(fridgeItem,DatabaseEventType.REMOVED)
                        }
                    }
                    removeFridgeItemTask.finishTask(fridgeItem)
                }
            }.addOnFailureListener { ex->
                removeFridgeItemTask.finishTask(ex)
            }
        }
        return removeFridgeItemTask
    }

    fun removeBundle(bundleToRemove: ShoppingBundle) : DatabaseItemTask
    {
        val removeBundleTask = DatabaseItemTask()
        bundleToRemove.databaseRef.delete().addOnSuccessListener {
            val dataType: KType = bundleToRemove::class.createType()
            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(bundleToRemove, DatabaseEventType.REMOVED)
                }
            }
            removeBundleTask.finishTask(bundleToRemove)
        }
        return removeBundleTask
    }

    fun addNewBundle(name: String, itemsInBundle: List<BundleItem>) : DatabaseItemTask
    {
        val data = hashMapOf(
            "name" to name
        )
        val newBundleTask = DatabaseItemTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Bundles").add(data).addOnSuccessListener {
            val id = it.id
            val newBundle = ShoppingBundle(id, DatabaseField("name", name), itemsInBundle, it)

            val dataType: KType = newBundle::class.createType()
            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(newBundle,DatabaseEventType.ADDED)
                }
            }
            newBundleTask.finishTask(newBundle)
        }
        return newBundleTask
    }

    fun addNewUserOrder(date: LocalDateTime, recurring: Boolean, daysToRepeat: Int) : DatabaseItemTask
    {
        val data = hashMapOf(
            "recurring" to recurring,
            "days_to_repeat" to daysToRepeat,
            "status" to "not processed",
            "date" to Timestamp(date.toEpochSecond(ZoneOffset.UTC),0)
        )
        val newUserOrderTask = DatabaseItemTask()
        db.collection("UserData").document(auth.uid.toString()).collection("Orders").add(data).addOnSuccessListener {
            val id = it.id
            val newUserOrder = UserOrder(id, emptyList(),
                DatabaseField("date",date),
                DatabaseField("days_to_repeat",daysToRepeat),
                DatabaseField("recurring",recurring),
                DatabaseField("status","not processed"),
                it)

            val dataType: KType = newUserOrder::class.createType()
            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(newUserOrder,DatabaseEventType.ADDED)
                }
            }
            newUserOrderTask.finishTask(newUserOrder)
        }
        return newUserOrderTask
    }

    fun removeUserOrder(orderToRemove: UserOrder): DatabaseItemTask
    {
        val removeOrderTask = DatabaseItemTask()
        orderToRemove.databaseRef.delete().addOnSuccessListener {
            val dataType: KType = orderToRemove::class.createType()
            if (modifyListeners.containsKey(dataType)) {
                for (listener in modifyListeners[dataType]!!) {
                    listener(orderToRemove, DatabaseEventType.REMOVED)
                }
            }
            removeOrderTask.finishTask(orderToRemove)
        }
        return removeOrderTask
    }

    fun sendPhoneVerificationCode(code: String)
    {
        val data = hashMapOf(
            "code" to code,
            "status" to 0
        )
        db.collection("VerificationCodes").document(auth.uid.toString()).set(data)
    }

    fun setOnPhoneVerificationStatusChangeListener(listener: (status:Int)->Unit)
    {
        db.collection("VerificationCodes").document(auth.uid.toString()).addSnapshotListener { doc, error ->
            if (error != null) {
                Log.w(TAG, "Listen failed.", error)
                return@addSnapshotListener
            }
            var newStatus = doc?.data?.get("status")
            if (newStatus != null && newStatus is Number)
            {
                listener(newStatus.toInt())
            }
            else
            {
                listener(0)
            }
        }
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

    fun loadPreferencesFromFirebase(context: Context)
    {
        val personalDataKeys = listOf("address","first_name","last_name","phone_number")

        db.collection("UserData").document(auth.uid.toString()).collection("Settings").document("personal_info").get().addOnSuccessListener {
            val data = it.data
            val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            for (key in personalDataKeys)
            {
                if (data != null && data.containsKey(key))
                {
                    editor.putString(key, data[key].toString())
                }
                else
                {

                }
            }
            editor.apply()
        }
        val deliveryKeys = listOf("time_selection_monday","time_selection_tuesday","time_selection_wednesday",
            "time_selection_thursday","time_selection_friday","time_selection_saturday","time_selection_sunday")
        db.collection("UserData").document(auth.uid.toString()).collection("Settings").document("delivery_info").get().addOnSuccessListener {
            val data = it.data
            val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            for(key in deliveryKeys)
            {
                if (data != null && data.containsKey(key))
                {
                    val valueFromDb = data[key] as ArrayList<String>
                    editor.putStringSet(key,valueFromDb.toHashSet())
                }
                else
                {
                    editor.putStringSet(key, emptySet())
                }
            }
            editor.apply()
        }
    }

    fun updatePersonalInfo(key: String, newValue: String): Task<Void> {
        return db.collection("UserData").document(auth.uid.toString()).collection("Settings").document("personal_info").update(key,newValue)
    }

    fun updateDeliveryInfo(key: String, newValues: HashSet<String>): Task<Void> {
        val valuesArray = newValues.toList()
        val data = hashMapOf(
            key to valuesArray
        )
        return db.collection("UserData").document(auth.uid.toString()).collection("Settings").document("delivery_info").set(data,SetOptions.merge())
    }

    //endregion

    enum class DatabaseEventType {
        MODIFIED,
        ADDED,
        REMOVED
    }

    private fun documentSafeToParse(objType: KType, doc: DocumentSnapshot) : Boolean
    {
        if (doc.data == null)
        {
            return false
        }
        var dbNames: List<String> = emptyList()
        when(objType)
        {
            Product::class.createType() -> {
                dbNames = listOf("name", "price", "id", "barcode")
            }
            QuantityItem::class.createType() -> {
                dbNames = listOf("measuring_unit","quantity","product")
            }
            FridgeItem::class.createType() -> {
                dbNames = listOf("measuring_unit","quantity","product")
            }
            BundleItem::class.createType() -> {
                dbNames = listOf("measuring_unit","quantity","product")
            }
            DeliveryItem::class.createType() -> {
                dbNames = listOf("measuring_unit","quantity","product")
            }
            ShoppingBundle::class.createType() -> {
                dbNames = listOf("name")
            }
            UserOrder::class.createType() -> {
                dbNames = listOf("date", "days_to_repeat", "recurring", "status")
            }
            Delivery::class.createType() -> {
                dbNames = listOf("date", "end_date", "status",)
            }
            else -> {
                return false}
        }
        for (name in dbNames)
        {
            if (!doc.data!!.containsKey(name))
            {
                return false
            }
        }
        return true
    }

    class DatabaseField<T>(val dbName: String, private var dbVal: T)
    {
        enum class DatabaseFieldEventType
        {
            DOWNLOAD,
            UPLOAD
        }

        private val onChangeListeners: MutableList<(T, DatabaseFieldEventType) -> Unit> = mutableListOf()
        private var valueFilter: (T)->T = {x -> x}
        var databaseValue = dbVal
            set(value){
                field= valueFilter(value)
                dbVal = valueFilter(value)
                for(listener in onChangeListeners)
                {
                    listener(dbVal, DatabaseFieldEventType.UPLOAD)
                }
            }
            get() = dbVal

        fun setValueFilter(filter: (T)->T)
        {
            valueFilter = filter
        }

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
                val valueToSetInDb: Any
                if(fieldChanged.databaseValue is LocalDateTime)
                {
                    val dateTime = fieldChanged.databaseValue as LocalDateTime
                    val zonedDateTime = dateTime.atZone(ZoneId.systemDefault())
                    valueToSetInDb = Timestamp(Date.from(zonedDateTime.toInstant()))
                }
                else
                {
                    valueToSetInDb = fieldChanged.databaseValue
                }
                databaseRef.update(fieldChanged.dbName, valueToSetInDb)
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

    class Product(id: String, val name: DatabaseField<String>, val price: DatabaseField<Double>, val storeGivenId: DatabaseField<String>, val barcode: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef), SearchSuggestion
    {
        constructor(parcel: Parcel) : this(
            "1",
            DatabaseField<String>("name",parcel.readString()!!),
            DatabaseField<Double>("",-1.0),
            DatabaseField<String>("","-1"),
            DatabaseField<String>("","-1"),
            db.collection("1").document("1"),
        )

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

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name.databaseValue)
        }

        override fun getBody(): String {
            return name.databaseValue
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Product> {
            override fun createFromParcel(parcel: Parcel): Product {
                return Product(parcel)
            }

            override fun newArray(size: Int): Array<Product?> {
                return arrayOfNulls(size)
            }
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
            quantity.setValueFilter { x->
                if(x <= 0)
                {
                    databaseRef.delete()
                    if(this is FridgeItem)
                    {
                        val dataType: KType = FridgeItem::class.createType()
                        if (modifyListeners.containsKey(dataType)) {
                            for (listener in modifyListeners[dataType]!!) {
                                listener(this,DatabaseEventType.REMOVED)
                            }
                        }
                    }
                    return@setValueFilter 0
                }
                else
                {
                    return@setValueFilter x
                }
            }
        }
    }

    class FridgeItem(id: String, measuringUnit: DatabaseField<String>, product: Product, quantity: DatabaseField<Int>, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)

    class BundleItem(id: String, measuringUnit: DatabaseField<String>, product: Product, quantity: DatabaseField<Int>, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)

    class DeliveryItem(id: String, measuringUnit: DatabaseField<String>, product: Product, quantity: DatabaseField<Int>, databaseRef: DocumentReference) : QuantityItem(id, measuringUnit, product, quantity, databaseRef)

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

    class UserOrder(id: String, shoppingBundles: List<ShoppingBundle>, val date: DatabaseField<LocalDateTime>, val daysToRepeat: DatabaseField<Int>, val recurring: DatabaseField<Boolean>, val status: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
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

    class Delivery(id: String, val date: DatabaseField<LocalDateTime>, val endDate: DatabaseField<LocalDateTime>, userOrders: List<UserOrder>, deliveryItems:List<DeliveryItem>, val status: DatabaseField<String>, databaseRef: DocumentReference) : DatabaseItem(id, databaseRef)
    {
        var userOrders: List<UserOrder> = userOrders
            private set
        var deliveryItems: List<DeliveryItem> = deliveryItems
            private set
        init {
            date.addOnChangeListener {v, t -> notifyFieldListeners(date.eraseType(),t) }
            date.bindToDatabaseListner(databaseRef)
            endDate.addOnChangeListener {v, t -> notifyFieldListeners(endDate.eraseType(),t) }
            endDate.bindToDatabaseListner(databaseRef)
            status.addOnChangeListener {v, t -> notifyFieldListeners(status.eraseType(),t) }
            status.bindToDatabaseListner(databaseRef)

            for(userOrder in userOrders)
            {
                userOrder.addOnFieldChangeListener { notifySubitemListeners(userOrder, DatabaseEventType.MODIFIED) }
                userOrder.addOnSubitemChangeListener {v,t -> notifySubitemListeners(userOrder, DatabaseEventType.MODIFIED) }
            }

            for (deliveryItem in deliveryItems)
            {
                deliveryItem.addOnFieldChangeListener { notifySubitemListeners(deliveryItem, DatabaseEventType.MODIFIED) }
                deliveryItem.addOnSubitemChangeListener { v,t ->  notifySubitemListeners(deliveryItem, DatabaseEventType.MODIFIED)}
            }

            // Bind to database listener and listen if removed or changed
            databaseRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                if (!snapshot!!.exists())
                {
                    val dataType: KType = this::class.createType()
                    if (modifyListeners.containsKey(dataType)) {
                        for (listener in modifyListeners[dataType]!!) {
                            listener(this,DatabaseEventType.REMOVED)
                        }
                    }
                }
                else
                {
                    val dataType: KType = this::class.createType()
                    if (modifyListeners.containsKey(dataType)) {
                        for (listener in modifyListeners[dataType]!!) {
                            listener(this,DatabaseEventType.MODIFIED)
                        }
                    }
                }
            }

            databaseRef.collection("DeliveryItems").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }
                for(dc in snapshots!!.documentChanges)
                {
                    when(dc.type)
                    {
                        DocumentChange.Type.ADDED -> {
                            deliveryItemFromDocument(dc.document).addOnSuccessListener { ret1->
                                val deliveryItem = ret1 as DeliveryItem
                                if (this.deliveryItems.none{x->x.id == deliveryItem.id})
                                {
                                    this.deliveryItems = this.deliveryItems.plus(deliveryItem)
                                    notifySubitemListeners(deliveryItem, DatabaseEventType.ADDED)
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {}
                        DocumentChange.Type.REMOVED -> {}
                    }
                }
            }

            databaseRef.collection("UserOrders").addSnapshotListener { value, error ->
                // TODO Handle adding userorders to delivery
            }
        }
    }

    class DatabaseItemTask: Task<DatabaseItem>()
    {
        private var completedTask = false
        private var successTask = false
        private var currentException: Exception? = null
        private lateinit var taskResult: DatabaseItem
        private val onSuccessListeners: MutableList<OnSuccessListener<in DatabaseItem>> = mutableListOf()
        private val onFailListeners: MutableList<OnFailureListener> = mutableListOf()
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
            if(completedTask && successTask)
            {
                p0.onSuccess(result)
            }
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
            onFailListeners.add(p0)
            if (completedTask && !successTask && currentException != null)
            {
                p0.onFailure(currentException!!)
            }
            return this
        }

        override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<DatabaseItem> {
            TODO("Not yet implemented")
        }

        override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<DatabaseItem> {
            TODO("Not yet implemented")
        }

        private fun onTaskSuccess()
        {
            for(listener in onSuccessListeners)
            {
                listener.onSuccess(taskResult)
            }
        }

        private fun onTaskFail()
        {
            if (currentException != null) {
                for (listener in onFailListeners) {
                    listener.onFailure(currentException!!)
                }
            }
        }

        private fun onTaskCompleted()
        {
            completedTask = true
            if (successTask) {
                onTaskSuccess()
            } else {
                onTaskFail()
            }
        }

        fun finishTask(dbItem: DatabaseItem)
        {
            taskResult = dbItem
            successTask = true
            onTaskCompleted()
        }

        fun finishTask(exception: Exception)
        {
            Log.w(TAG, exception.toString())
            successTask = false
            currentException = exception
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

