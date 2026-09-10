import SwiftUI

/**
 The first thing a new install shows: sign in, or say you would rather not.

 It is a door, not a wall. Every square Ritual keeps lives on the device, and
 an account exists for exactly one reason — carrying a practice to the next
 phone. So the way past this screen without one is a plain, visible choice
 rather than a small grey line, and taking it costs nothing at all.

 It is also skipped outright when there is no Firebase configuration, because
 a sign-in nobody can complete is a locked door.
 */
struct WelcomeView: View {

    @ObservedObject var account: Account
    let onSignedIn: () -> Void
    let onSkip: () -> Void

    @State private var address = ""
    @State private var password = ""
    @State private var creating = true

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                hero.padding(.top, 28)

                Text("Sign in and every square you fill is mirrored to your account, so a new phone picks up exactly where the old one left off.")
                    .bodyStyle(15)
                    .padding(.top, 22)

                CapsLabel(text: "Email").padding(.top, 24)
                TextField("you@example.com", text: $address)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .fieldStyle()
                    .padding(.top, 8)

                CapsLabel(text: "Password").padding(.top, 16)
                SecureField("At least six characters", text: $password)
                    .textContentType(creating ? .newPassword : .password)
                    .fieldStyle()
                    .padding(.top, 8)

                if let error = account.error {
                    Text(error).bodyStyle(13, color: Theme.red).padding(.top, 12)
                }

                InkPill(
                    label: account.busy ? "Working…" : (creating ? "Create account" : "Sign in"),
                    action: {
                        Task {
                            let ok = creating
                                ? await account.createAccount(email: address, password: password)
                                : await account.signIn(email: address, password: password)
                            if ok, let uid = account.uid {
                                CloudSync.shared.start(uid: uid)
                                onSignedIn()
                            }
                        }
                    }
                )
                .padding(.top, 20)
                .disabled(account.busy)

                Button(creating ? "I already have an account" : "I need an account") {
                    creating.toggle()
                }
                .font(Theme.body(13))
                .foregroundStyle(Theme.inkSoft)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)

                InkPill(
                    label: "Use Ritual on this phone",
                    background: Theme.cream,
                    content: Theme.ink,
                    border: Theme.ink,
                    action: onSkip
                )
                .padding(.top, 20)

                Text("No account, no network, nothing to lose track of. You can sign in later from the cat in the corner.")
                    .bodyStyle(12, color: Theme.inkFaint)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 10)

                Spacer(minLength: 40)
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.cream)
    }

    private var hero: some View {
        VStack(spacing: 16) {
            MochiTile(mood: .pleased, tile: Theme.paper, height: 64, corner: 20, inset: 16)
            Text("A year is a grid\nof empty squares.")
                .displayStyle(26)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 26)
        .background(Theme.lime)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

private extension View {
    /// The one text field style in the app, which the account screen also uses.
    func fieldStyle() -> some View {
        self
            .font(Theme.body(16))
            .foregroundStyle(Theme.ink)
            .tint(Theme.ink)
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .background(Theme.paper)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}
