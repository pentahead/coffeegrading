package id.my.faruq.coffegrader.ui.sample

data class SampleInputState(
    val batchId: String = "",
    val coffeeType: String = "Robusta",
    val processingMethod: String = "Dry",
    val beanSize: String = "Besar",
    val beanShape: String = "Normal",

    val hasInsect: Boolean = false,
    val hasMoldSmell: Boolean = false,

    val moisture: Float = 0f,
    val dirt: Float = 0f,

    val sortationType: String = "Primer"
)
