import SwiftUI

struct ContentView: View {
    @StateObject private var vm = PrayerGuidanceViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Prayer Companion")
                    .font(.title2)
                    .bold()

                Picker("Prayer", selection: $vm.selectedPrayer) {
                    ForEach(vm.prayers, id: \.self) { p in
                        Text(p).tag(p)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: vm.selectedPrayer) { _, newPrayer in
                    vm.selectPrayer(newPrayer)
                }

                Group {
                    Text("Current Step: \(vm.snapshot.currentStep)")
                    Text("Previous Step: \(vm.snapshot.previousStep ?? "-")")
                    Text("Next Step: \(vm.snapshot.nextStep ?? "-")")
                    Text("Rakah: \(vm.snapshot.currentRakah)/\(vm.snapshot.totalRakah)")
                    Text("Completed: \(vm.snapshot.completedRakah)")
                    Text("Posture: \(vm.snapshot.posture)")
                    Text("Confidence: \(Int(vm.snapshot.confidence * 100))%")
                }
                .font(.callout)

                if let lastEvent = vm.snapshot.lastEvent, !lastEvent.isEmpty {
                    Text(lastEvent)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                Divider()

                Text("Manual Posture Feed (starter)")
                    .font(.headline)

                LazyVGrid(
                    columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ],
                    spacing: 8
                ) {
                    ForEach(vm.postureInputs, id: \.self) { posture in
                        Button(posture) {
                            vm.feedPosture(posture)
                        }
                        .buttonStyle(.bordered)
                    }
                }

                Divider()

                HStack(spacing: 12) {
                    Button("Salaam Right") { vm.salaamRight() }
                        .buttonStyle(.borderedProminent)
                    Button("Salaam Left") { vm.salaamLeft() }
                        .buttonStyle(.bordered)
                }
            }
            .padding()
        }
    }
}

#Preview {
    ContentView()
}

