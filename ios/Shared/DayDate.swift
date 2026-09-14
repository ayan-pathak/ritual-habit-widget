import Foundation

/**
 A calendar day: no clock, no timezone.

 Completions are stored as epoch-days, one `Int` each, exactly as the Android
 build stores them — the same JSON blob would read on either platform. The
 arithmetic is Hinnant's civil algorithm rather than `Calendar` so that a day
 stays a day whatever the device's zone is doing, which is what `java.time.
 LocalDate` guarantees on the other side.
 */
struct DayDate: Hashable, Comparable {

    let year: Int
    let month: Int
    let day: Int
    let epochDay: Int

    init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
        self.epochDay = DayDate.epochDay(year: year, month: month, day: day)
    }

    init(epochDay: Int) {
        let c = DayDate.civil(fromEpochDay: epochDay)
        self.year = c.y
        self.month = c.m
        self.day = c.d
        self.epochDay = epochDay
    }

    static func today() -> DayDate {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        return DayDate(year: c.year ?? 1970, month: c.month ?? 1, day: c.day ?? 1)
    }

    static func < (a: DayDate, b: DayDate) -> Bool { a.epochDay < b.epochDay }

    func adding(days: Int) -> DayDate { DayDate(epochDay: epochDay + days) }

    /// Sunday = 0, which is the order the grid's rows run in.
    var weekdaySundayFirst: Int { ((epochDay + 4) % 7 + 7) % 7 }

    var dayOfYear: Int { epochDay - DayDate(year: year, month: 1, day: 1).epochDay + 1 }

    static func isLeap(_ y: Int) -> Bool { (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 }

    static func lengthOfYear(_ y: Int) -> Int { isLeap(y) ? 366 : 365 }

    static func lengthOfMonth(_ m: Int, in y: Int) -> Int {
        switch m {
        case 1, 3, 5, 7, 8, 10, 12: return 31
        case 4, 6, 9, 11: return 30
        default: return isLeap(y) ? 29 : 28
        }
    }

    private static func epochDay(year: Int, month m: Int, day d: Int) -> Int {
        let y = year - (m <= 2 ? 1 : 0)
        let era = (y >= 0 ? y : y - 399) / 400
        let yoe = y - era * 400                                    // [0, 399]
        let doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1   // [0, 365]
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy            // [0, 146096]
        return era * 146097 + doe - 719468
    }

    private static func civil(fromEpochDay epochDay: Int) -> (y: Int, m: Int, d: Int) {
        let z = epochDay + 719468
        let era = (z >= 0 ? z : z - 146096) / 146097
        let doe = z - era * 146097                                 // [0, 146096]
        let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        let y = yoe + era * 400
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)          // [0, 365]
        let mp = (5 * doy + 2) / 153                               // [0, 11]
        let d = doy - (153 * mp + 2) / 5 + 1                       // [1, 31]
        let m = mp + (mp < 10 ? 3 : -9)                            // [1, 12]
        return (y + (m <= 2 ? 1 : 0), m, d)
    }
}
