package challenge

import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class Order(val customer: Customer, val address: Address) {
    private val items = mutableListOf<OrderItem>()
    var closedAt: Date? = null
        private set
    var payment: Payment? = null
        private set
    val totalAmount
        get() = items.sumByDouble { it.total }

    fun addProduct(product: Product, quantity: Int) {
        var productAlreadyAdded = items.any { it.product == product }
        if (productAlreadyAdded)
            throw Exception("The product have already been added. Change the amount if you want more.")

        items.add(OrderItem(product, quantity))
    }

    fun pay(method: PaymentMethod) {
        if (payment != null)
            throw Exception("The order has already been paid!")

        if (items.count() == 0)
            throw Exception("Empty order can not be paid!")

        payment = Payment(this, method)

        close()
    }

    fun deliver () {
        if (items.count() == 0)
            throw Exception("Empty order can not be deliver!")

        var shippingRules = ShippingRules()

        items.forEach {
           println(shippingRules.shippings.get(it.product.type)?.submit(customer, address))
        }
    }

    private fun close() {
        closedAt = Date()
    }
}

data class OrderItem(val product: Product, val quantity: Int) {
    val total get() = product.price * quantity
}

data class Payment(val order: Order, val paymentMethod: PaymentMethod) {
    val paidAt = Date()
    val authorizationNumber = paidAt.time
    val amount = order.totalAmount
    val invoice = Invoice(order)
}

data class CreditCard(val number: String) : PaymentMethod

interface PaymentMethod

data class Invoice(val order: Order) {
    val billingAddress: Address = order.address
    val shippingAddress: Address = order.address
}

data class Product(val name: String, val type: ProductType, val price: Double)

enum class ProductType {
    PHYSICAL,
    BOOK,
    DIGITAL,
    MEMBERSHIP
}

class Address (val zipCode : String)

class Customer(val email: String)

data class Voucher (val value: Double) {
    fun toDiscount() : Double {
        if (value <= 10.0)
            throw Exception("It is not possible to discount a value less than or equal to R$ 10")

        return value - 10.0
    }
}

class Email (val to: String, val from: String) {
    fun send(messages: String) {
       println("To: ${to}, From: ${from},  ${messages}")
    }
}

interface ShippingLabel {
    fun generate(address: Address)
}

class PhysicalItemLabel: ShippingLabel {
    override fun generate(address: Address) {
        println("Print address of customer, field zipcode: ${address.zipCode}")
    }
}

class BookLabel: ShippingLabel {
    override fun generate(address: Address) {
        println("Print address of customer, field zipcode: ${address.zipCode}")
        println("Tax exempt as provided in the Constitution Art. 150, VI, d")
    }
}

class GeneratorShippingLabel {
    val labels: Map<String, ShippingLabel> = HashMap(hashMapOf("PHYSICAL" to PhysicalItemLabel(), "BOOK" to BookLabel()))
}

interface Shipping {
    fun submit(customer: Customer, address: Address) : String
}

class ShippingPhysicalItem:Shipping {
    override fun submit(customer: Customer, address: Address) :String {
        GeneratorShippingLabel().labels["PHYSICAL"]?.generate(address)
        return "It physical item"
    }
}

class ShippingServiceSubscription : Shipping {
    override fun submit(customer: Customer, address: Address) :String {
        var email = Email(to= "ecommerce@ecommer.com", from= customer.email)
        email.send("Your subscription has been activated")
        return "It services subscription"
    }
}

class ShippingBook:Shipping {
    override fun submit(customer: Customer, address: Address) :String {
        GeneratorShippingLabel().labels["BOOK"]?.generate(address)
        return "It book"
    }
}

class ShippingDigitalMedia:Shipping {
    override fun submit(customer: Customer, address: Address) :String {
        applyVoucher(20.0)
        var email = Email(to= "ecommerce@ecommer.com", from= customer.email)
        email.send("Your subscription has been activated")
        return "It digital media"
    }

    private fun applyVoucher(value: Double) {
        var voucher = Voucher(value)
        voucher.toDiscount()
    }
}

class ShippingRules {
    val shippings: Map<ProductType, Shipping> = HashMap(hashMapOf(
            ProductType.PHYSICAL to ShippingPhysicalItem(),
            ProductType.MEMBERSHIP to ShippingServiceSubscription(),
            ProductType.BOOK to ShippingBook(),
            ProductType.DIGITAL to ShippingDigitalMedia()
    ))
}

fun main(args : Array<String>) {
    val shirt = Product("Flowered t-shirt", ProductType.PHYSICAL, 35.00)
    val netflix = Product("Familiar plan", ProductType.MEMBERSHIP, 29.90)
    val book = Product("The Hitchhiker's Guide to the Galaxy", ProductType.BOOK, 120.00)
    val music = Product("Stairway to Heaven", ProductType.DIGITAL, 5.00)

    val order = Order(Customer(email= "foolano@gmail.com"), Address(zipCode= "00000-000"))

    order.addProduct(shirt, 2)
    order.addProduct(netflix, 1)
    order.addProduct(book, 1)
    order.addProduct(music, 1)

    order.pay(CreditCard("43567890-987654367"))
    // now, how to deal with shipping rules then?
    order.deliver()
}