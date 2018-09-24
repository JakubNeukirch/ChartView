# ChartView
Simple ChartView for Android.

![screenshot](https://github.com/JakubNeukirch/ChartView/blob/master/Screenshot_1537704930.png)

## Installation
In your root build.gradle file add this:
```
repositories{
    maven { url 'https://jitpack.io' }
}
```
To your app module add dependency:
```
implementation 'com.github.JakubNeukirch:ChartView:0.5.1'
```
## Usage
Usage is shown in app/src/main/java/pl/kuben/chart/MainActivity

To use it simply add PeriodicChartView to your xml. Available attributes:
- `valueText: String` - identifier at the top of chart - default "val."
- `dateText: String` - identifier for date - default "date"
- `showText: Boolean` - defines if text should be shown, mostly used when chart is small - default "true"
- `progressColor: @IntColor Int` - defines value line color - default "blue"
- `chartPadding: Int` - defines padding of drawed chart - default "10px"
- `dateInterval: Enum` - defines how entries should be grouped - available "day", "week", "month" - default "day"

Also available variables:
- `fromDate: Long` - timestamp date which limits range of values
- `toDate: Long` - timestamp date which limits range of values

To add values you need to insert list of  `Entry` which has two field:
`count: Int` - the value which it contains
`date: Long` - timestamp of adding it
Inserting values:
```kotlin
        chart.entries = listOf(
                Entry(2, System.currentTimeMillis()),
                Entry(4, System.currentTimeMillis() + 1000 * 60 * 60 * 24),
                Entry(5, System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 2),
                Entry(1, System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 3)
        )
```
  
  Finally entries are summed up and sorted so it does not need to be sorted or added in any special order.
  
  ## Site
[www.jakubneukirch.pl](https://jakubneukirch.pl/chartview/)
