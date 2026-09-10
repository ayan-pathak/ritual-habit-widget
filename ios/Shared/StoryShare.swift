import UIKit

/**
 Puts a streak card into an Instagram story.

 Instagram's story URL is the good path: it drops the image straight onto the
 story canvas with the app's colours behind it. It is only available when
 Instagram is installed, so every call falls back to the system share sheet
 rather than dead-ending.

 The card goes over as the **background asset only**. Passing the same image a
 second time as a sticker makes Instagram composite it twice — the background
 scaled to fill a taller-than-9:16 screen, the sticker sitting on top of it —
 which shows up as the background's edges peeking out down the left and right
 of the card. One asset, no doubling.
 */
enum StoryShare {

    private static let storyURL = URL(string: "instagram-stories://share")!

    static var isInstagramInstalled: Bool {
        UIApplication.shared.canOpenURL(storyURL)
    }

    /// Renders `model` and hands it to Instagram Stories. Returns false when
    /// Instagram can't take it, so the caller can offer the share sheet.
    @discardableResult
    @MainActor
    static func shareStreak(model: SlabModel) -> Bool {
        guard isInstagramInstalled else { return false }
        guard let png = ShareCardRenderer.render(model: model).pngData() else { return false }

        let bundleID = Bundle.main.bundleIdentifier ?? "com.ayan.ritual"
        guard var components = URLComponents(url: storyURL, resolvingAgainstBaseURL: false) else {
            return false
        }
        components.queryItems = [URLQueryItem(name: "source_application", value: bundleID)]
        guard let url = components.url else { return false }

        // The two brand colours become the story's backdrop, filling whatever
        // the card does not cover on a taller screen.
        let items: [String: Any] = [
            "com.instagram.sharedSticker.backgroundImage": png,
            "com.instagram.sharedSticker.backgroundTopColor": "#E7E3D4",
            "com.instagram.sharedSticker.backgroundBottomColor": "#C9F73F"
        ]
        UIPasteboard.general.setItems(
            [items],
            options: [.expirationDate: Date().addingTimeInterval(60 * 5)]
        )
        UIApplication.shared.open(url)
        return true
    }

    /// The image itself, for the system share sheet.
    static func card(for model: SlabModel) -> UIImage {
        ShareCardRenderer.render(model: model)
    }
}
