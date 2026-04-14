import Foundation
import SharedKit

@MainActor
final class PrayerGuidanceViewModel: ObservableObject {
    private let facade = PrayerSessionFacade(initialPrayer: "FAJR")

    @Published var snapshot: GuidanceSnapshot
    @Published var selectedPrayer: String

    init() {
        self.snapshot = facade.current()
        self.selectedPrayer = snapshot.prayer
    }

    var prayers: [String] {
        facade.availablePrayers()
    }

    var postureInputs: [String] {
        facade.availablePostures()
    }

    func selectPrayer(_ prayer: String) {
        snapshot = facade.setPrayer(prayer: prayer)
        selectedPrayer = prayer
    }

    func feedPosture(_ posture: String, confidence: Float = 0.90) {
        snapshot = facade.onDetectedPosture(posture: posture, confidence: confidence)
    }

    func salaamRight() {
        snapshot = facade.markSalaamRight()
    }

    func salaamLeft() {
        snapshot = facade.markSalaamLeft()
    }
}

