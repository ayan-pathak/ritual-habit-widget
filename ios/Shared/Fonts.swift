import CoreText
import UIKit

/**
 Archivo, in the three weights the design uses.

 One variable TTF ships in the bundle and the `wght` axis is pinned per weight,
 so the Core Graphics renderer and SwiftUI draw the very same instances — the
 iOS equivalent of the `archivo_*.xml` font families on Android. A missing or
 unloadable font must never take the widget down with it, so every path falls
 back to the system face.
 */
enum Fonts {

    private static let weightAxis: Int = 0x77676874  // 'wght'
    private static var registered = false

    /// Registers the bundled TTF with Core Text. Safe to call repeatedly.
    static func register() {
        if registered { return }
        registered = true
        let bundle = Bundle(for: FontsAnchor.self)
        guard let url = bundle.url(forResource: "Archivo", withExtension: "ttf") else { return }
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
    }

    static func extraBold(_ size: CGFloat) -> UIFont { archivo(size, weight: 800, fallback: .heavy) }
    static func semiBold(_ size: CGFloat) -> UIFont { archivo(size, weight: 600, fallback: .semibold) }
    static func medium(_ size: CGFloat) -> UIFont { archivo(size, weight: 500, fallback: .medium) }

    private static func archivo(_ size: CGFloat, weight: CGFloat, fallback: UIFont.Weight) -> UIFont {
        register()
        let base = UIFont(name: "Archivo", size: size)
            ?? UIFont(name: "Archivo-Regular", size: size)
            ?? UIFont(name: "Archivo-Variable", size: size)
        guard let base else { return .systemFont(ofSize: size, weight: fallback) }
        let descriptor = base.fontDescriptor.addingAttributes([
            UIFontDescriptor.AttributeName(rawValue: kCTFontVariationAttribute as String): [weightAxis: weight]
        ])
        return UIFont(descriptor: descriptor, size: size)
    }
}

/// Only here so `Bundle(for:)` finds whichever bundle this file was built into
/// — the app and the widget extension each carry their own copy of the font.
private final class FontsAnchor {}
