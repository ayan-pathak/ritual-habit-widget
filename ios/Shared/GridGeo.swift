import Foundation

/**
 The commit-grid coordinate system: weeks run left to right, weekdays top to
 bottom, Sunday first — the arrangement everyone already knows how to read.
 */
enum GridGeo {

    static let rows = 7

    /// Row that Jan 1 of `year` occupies. Sunday = 0.
    static func startOffset(_ year: Int) -> Int {
        DayDate(year: year, month: 1, day: 1).weekdaySundayFirst
    }

    /// Number of week-columns needed to hold `year`.
    static func colsFor(_ year: Int) -> Int {
        (startOffset(year) + DayDate.lengthOfYear(year) - 1) / rows + 1
    }

    /// Column of the week containing `date`.
    static func colOf(_ date: DayDate) -> Int {
        (startOffset(date.year) + date.dayOfYear - 1) / rows
    }

    /// Row (weekday) of `date`.
    static func rowOf(_ date: DayDate) -> Int {
        (startOffset(date.year) + date.dayOfYear - 1) % rows
    }

    static func lengthOf(_ year: Int) -> Int { DayDate.lengthOfYear(year) }
}

let MONTH_INITIALS = ["J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"]
let MONTH_SHORT = [
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
]
