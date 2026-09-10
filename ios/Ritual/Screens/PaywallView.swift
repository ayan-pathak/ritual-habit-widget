import SwiftUI

/**
 The wall, and the only one in the app.

 It appears when someone reaches for a second ritual, which is the moment the
 app has already proved itself — never on launch, and never in front of a day
 waiting to be marked. Everything already kept stays free and stays visible.
 */
struct PaywallView: View {
    @ObservedObject var unlock: Unlock
    let onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Spacer()
                    RoundButton(size: 40, action: onClose) {
                        Path { path in
                            path.move(to: CGPoint(x: 5, y: 5))
                            path.addLine(to: CGPoint(x: 13, y: 13))
                            path.move(to: CGPoint(x: 13, y: 5))
                            path.addLine(to: CGPoint(x: 5, y: 13))
                        }
                        .stroke(Theme.ink, style: StrokeStyle(lineWidth: 2, lineCap: .round))
                        .frame(width: 18, height: 18)
                    }
                }
                .padding(.top, 14)

                VStack(alignment: .center, spacing: 18) {
                    MochiTile(mood: .pleased, tile: Theme.paper, pixel: 2.5, corner: 18, inset: 14)
                    Text("Keep more than one.")
                        .displayStyle(28)
                        .multilineTextAlignment(.center)
                    Text("Your first ritual is free forever. Unlock the rest once, and they are yours for good.")
                        .bodyStyle(14, color: Color(0xB312120F as ARGB))
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(24)
                .background(Theme.lime)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.top, 8)

                VStack(alignment: .leading, spacing: 14) {
                    ForEach(Self.included, id: \.self) { line in
                        HStack(alignment: .top, spacing: 10) {
                            Checkmark(size: 15).padding(.top, 3)
                            Text(line).bodyStyle(14, color: Theme.ink)
                        }
                    }
                }
                .padding(.top, 26)

                VStack(spacing: 10) {
                    InkPill(
                        label: unlock.purchasing
                            ? "Working…"
                            : "Unlock forever · \(unlock.displayPrice)",
                        action: { Task { await unlock.purchase() } }
                    )
                    .disabled(unlock.purchasing)

                    Button("Restore a previous purchase") {
                        Task { await unlock.restore() }
                    }
                    .font(Theme.body(13))
                    .foregroundStyle(Theme.inkSoft)
                }
                .padding(.top, 26)

                if let error = unlock.lastError {
                    Text(error)
                        .bodyStyle(12, color: Theme.red)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 12)
                }

                Text("One payment. No subscription, no account, nothing to cancel.")
                    .bodyStyle(12, color: Theme.inkFaint)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 18)
                    .padding(.bottom, 40)
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.cream)
        .onChange(of: unlock.isUnlocked) { _, unlocked in
            if unlocked { onClose() }
        }
    }

    private static let included = [
        "As many rituals as you keep",
        "A widget for each one",
        "Every year you have kept, in the archive",
        "Story cards without a watermark"
    ]
}
