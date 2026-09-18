import AuthenticationServices
import SwiftUI

/**
 The account, which is only ever about one thing: carrying a practice from one
 phone to the next.

 Signing in is optional and stays optional. Everything Ritual does works with
 no account on a device that has never seen a network, and nothing here is a
 step anyone has to take before keeping a day.
 */
struct AccountView: View {
    @ObservedObject var account: Account
    let onBack: () -> Void

    @State private var address = ""
    @State private var password = ""
    @State private var creating = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    RoundButton(size: 40, action: onBack) { Chevron() }
                    Spacer()
                }
                .padding(.top, 14)

                Text(account.uid == nil ? "Keep your\nsquares safe." : "Your rituals\nfollow you.")
                    .displayStyle(34)
                    .padding(.top, 18)

                Text(explanation)
                    .bodyStyle(14)
                    .padding(.top, 12)

                if account.uid == nil {
                    signedOut
                } else {
                    signedIn
                }

                Spacer(minLength: 40)
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.cream)
    }

    private var explanation: String {
        if let email = account.email {
            return "Signed in as \(email). Every ritual on this phone is mirrored to your account, and any phone you sign in on picks them up."
        }
        if !account.available {
            return "This build has no Firebase configuration, so there is nothing to sign in to yet. Everything else works exactly as it does."
        }
        return "An account exists so a new phone can pick up where the old one left off. Ritual works perfectly well without one."
    }

    private var signedOut: some View {
        VStack(alignment: .leading, spacing: 0) {
            if account.available {
                VStack(spacing: 10) {
                    SignInWithAppleButton(.continue) { request in
                        account.prepare(request)
                    } onCompletion: { result in
                        Task { landed(await account.finish(result)) }
                    }
                    .signInWithAppleButtonStyle(.black)
                    .frame(height: 58)
                    .clipShape(Capsule())

                    if account.googleAvailable {
                        InkPill(
                            label: "Continue with Google",
                            background: Theme.paper,
                            content: Theme.ink,
                            border: Theme.ink,
                            action: { Task { landed(await account.signInWithGoogle()) } }
                        )
                    }
                }
                .padding(.top, 26)

                Text("or use an email")
                    .bodyStyle(12, color: Theme.inkFaint)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 22)
            }

            CapsLabel(text: "Email").padding(.top, account.available ? 18 : 26)
            TextField("you@example.com", text: $address)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .font(Theme.body(16))
                .foregroundStyle(Theme.ink)
                .tint(Theme.ink)
                .padding(.horizontal, 18)
                .padding(.vertical, 16)
                .background(Theme.paper)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .padding(.top, 8)

            CapsLabel(text: "Password").padding(.top, 16)
            SecureField("At least six characters", text: $password)
                .textContentType(creating ? .newPassword : .password)
                .font(Theme.body(16))
                .foregroundStyle(Theme.ink)
                .tint(Theme.ink)
                .padding(.horizontal, 18)
                .padding(.vertical, 16)
                .background(Theme.paper)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .padding(.top, 8)

            if let error = account.error {
                Text(error).bodyStyle(13, color: Theme.red).padding(.top, 12)
            }

            InkPill(
                label: account.busy ? "Working…" : (creating ? "Create account" : "Sign in"),
                action: {
                    Task {
                        landed(creating
                            ? await account.createAccount(email: address, password: password)
                            : await account.signIn(email: address, password: password))
                    }
                }
            )
            .padding(.top, 20)
            .disabled(account.busy || !account.available)

            Button(creating ? "I already have an account" : "I need an account") {
                creating.toggle()
            }
            .font(Theme.body(13))
            .foregroundStyle(Theme.inkSoft)
            .frame(maxWidth: .infinity)
            .padding(.top, 12)
        }
    }

    private func landed(_ ok: Bool) {
        guard ok, let uid = account.uid else { return }
        CloudSync.shared.start(uid: uid)
        onBack()
    }

    private var signedIn: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 14) {
                MochiTile(mood: .pleased, tile: Theme.paper, height: 32, corner: 14, inset: 9)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Mirrored").displayStyle(19)
                    Text("Every change on this phone is written up as it happens.")
                        .bodyStyle(12)
                }
            }
            .padding(22)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.lime)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .padding(.top, 26)

            InkPill(
                label: "Sign out",
                background: Theme.cream,
                content: Theme.ink,
                border: Theme.ink,
                action: {
                    CloudSync.shared.stop()
                    account.signOut()
                }
            )
            .padding(.top, 16)

            Text("Signing out leaves every ritual on this phone exactly where it is.")
                .bodyStyle(12, color: Theme.inkFaint)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 10)
        }
    }
}
