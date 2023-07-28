package com.projecturanus.betterp2p.client.gui

import com.projecturanus.betterp2p.client.gui.widget.WidgetScrollBar
import com.projecturanus.betterp2p.network.data.P2PLocation
import kotlin.reflect.KProperty0

/**
 * InfoList
 * Dedicated list to hold "InfoWrappers". Internally, stores
 * them in a HashMap. This is an opaque type, and access to
 * it is restricted. External access is instead directed to
 * a sorted view and a filtered view of the internal map.
 */
class InfoList (initList: Collection<InfoWrapper>,
                var hideIn: KProperty0<Boolean>,
                var hideOut: KProperty0<Boolean>,
                var hideBound: KProperty0<Boolean>,
                var hideUnbound: KProperty0<Boolean>) {

    /**
     * The master map, acts as the source of truth for all items
     * in this list.
     */
    private val masterMap: HashMap<P2PLocation, InfoWrapper> = hashMapOf()

    /**
     * Sorted view of the master map. This is resorted whenever
     * the map is updated.
     */
    val sorted: MutableList<InfoWrapper> = mutableListOf()

    /**
     * Filtered view of the sorted view (not the master map)
     */
    var filtered: List<InfoWrapper> = listOf()

    private val filter: InfoFilter = InfoFilter()


    val selectedInfo: InfoWrapper?
        get() {
            return if (selectedEntry == null) {
                null
            } else {
                masterMap[selectedEntry]
            }
       }

    var selectedEntry: P2PLocation? = null

    val size: Int
        get() = masterMap.size

    init {
        initList.forEach { masterMap[it.loc] = it }
    }

    fun resort() {
        sorted.sortBy {
            if (it.loc == selectedEntry) {
                -2 // Put the selected p2p at the front
                // Non-Zero frequencies
            } else if (it.frequency != 0L && it.frequency == selectedInfo?.frequency && !it.output) {
                -3 // Put input at the beginning
            } else if (it.frequency != 0.toLong() && it.frequency == selectedInfo?.frequency) {
                -1 // Put same frequency at the front
            } else {
                // Frequencies from lowest to highest
                it.frequency + Short.MAX_VALUE
            }
        }
    }

    /**
     * Updates the filtered list.
     */
    fun refilter(search: String) {
        filter.updateFilter(search.lowercase())
        filtered = sorted.filter {
            // It ain't pretty, but it's faster than before.
            if (it.loc == selectedEntry) {
                return@filter true
            }
            if (hideIn.get() && !it.output) {
                return@filter false
            }
            if (hideOut.get() && it.output) {
                return@filter false
            }
            if (hideBound.get() && it.frequency != 0L && !it.error) {
                return@filter false
            }
            if (hideUnbound.get() && (it.error || it.frequency == 0L)) {
                return@filter false
            }
            for ((f, strs) in filter.activeFilters) {
                if(!f.filter(it, strs?.toList())) {
                    return@filter false
                }
            }
            true
        }.sortedBy {
            when {
                it.loc == selectedEntry -> Long.MIN_VALUE + 1
                it.frequency != 0L && it.frequency == selectedInfo?.frequency && !it.output -> Long.MIN_VALUE
                // Put the same frequency to the front
                it.frequency != 0.toLong() && it.frequency == selectedInfo?.frequency -> Long.MIN_VALUE + 2
                filter.activeFilters.containsKey(Filter.NAME) -> {
                    var hits = 0L
                    var name = it.name
                    for (f in filter.activeFilters[Filter.NAME]!!) {
                        if (name.contains(f, true)) {
                            hits += 1
                            name = name.replaceFirst(f, "", true)
                        }
                    }
                    -(hits * hits) + name.length
                }
                else -> it.frequency + Short.MAX_VALUE - (if (it.output) 0 else 1)
            }
        }
    }

    /**
     * Updates the sorted list and applies the filter again.
     */
    fun refresh(search: String) {
        sorted.clear()
        sorted.addAll(masterMap.values)
        resort()
        refilter(search)
    }

    /**
     * Completely refresh the master list.
     */
    fun rebuild(updateList: Collection<InfoWrapper>, search: String, scrollbar: WidgetScrollBar, numEntries: Int) {
        masterMap.clear()
        updateList.forEach { masterMap[it.loc] = it }
        sorted.clear()
        sorted.addAll(masterMap.values)
        resort()
        refilter(search)
        scrollbar.setRange(0, masterMap.size.coerceIn(0, (masterMap.size - numEntries).coerceAtLeast(0)), 23)
    }

    /**
     * Updates the master list and sends the changes downstream to sorted/filtered.
     */
    fun update(updateList: Collection<InfoWrapper>, search: String, scrollbar: WidgetScrollBar, numEntries: Int) {
        updateList.forEach { masterMap[it.loc] = it }
        sorted.clear()
        sorted.addAll(masterMap.values)
        resort()
        refilter(search)
        scrollbar.setRange(0, masterMap.size.coerceIn(0, (masterMap.size - numEntries).coerceAtLeast(0)), 23)
    }

    fun select(which: P2PLocation?) {
        selectedEntry = masterMap.getOrDefault(which, null)?.loc
    }

    fun findInput(frequency: Long): InfoWrapper? {
        return masterMap.values.find { it.frequency == frequency && !it.output }
    }

    fun findAnyOutput(frequency: Long): InfoWrapper? {
        return masterMap.values.find { it.frequency == frequency && it.output }
    }
}
