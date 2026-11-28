import AVFoundation
import ComposeApp
import PhotosUI
import UIKit
import UniformTypeIdentifiers

final class NativeBridge: NSObject,
    IosNativeMediaAndShareBridge,
    UIImagePickerControllerDelegate,
    UINavigationControllerDelegate,
    PHPickerViewControllerDelegate,
    AVAudioPlayerDelegate,
    UIDocumentPickerDelegate {

    static let shared = NativeBridge()

    private var photoCallback: ((String?, String?, String?) -> Void)?
    private var importCallback: ((String?) -> Void)?
    private var audioRecorder: AVAudioRecorder?
    private var recordUrl: URL?
    private var recordStartDate: Date?
    private var audioPlayer: AVAudioPlayer?
    private var audioCompletedCallback: (() -> Void)?

    private func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            return nil
        }
        var current = root
        while let presented = current.presentedViewController {
            current = presented
        }
        return current
    }

    func pickPhoto(callback: @escaping (String?, String?, String?) -> Void) {
        DispatchQueue.main.async {
            guard let vc = self.topViewController() else { callback(nil, nil, nil); return }
            self.photoCallback = callback
            var config = PHPickerConfiguration()
            config.filter = .images
            config.selectionLimit = 1
            let picker = PHPickerViewController(configuration: config)
            picker.delegate = self
            vc.present(picker, animated: true)
        }
    }

    func capturePhoto(callback: @escaping (String?, String?, String?) -> Void) {
        DispatchQueue.main.async {
            guard UIImagePickerController.isSourceTypeAvailable(.camera),
                  let vc = self.topViewController() else {
                callback(nil, nil, nil)
                return
            }
            self.photoCallback = callback
            let picker = UIImagePickerController()
            picker.sourceType = .camera
            picker.delegate = self
            vc.present(picker, animated: true)
        }
    }

    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        guard let provider = results.first?.itemProvider,
              provider.canLoadObject(ofClass: UIImage.self) else {
            photoCallback?(nil, nil, nil)
            photoCallback = nil
            return
        }
        provider.loadObject(ofClass: UIImage.self) { object, _ in
            guard let image = object as? UIImage else {
                self.photoCallback?(nil, nil, nil)
                self.photoCallback = nil
                return
            }
            self.emitCompressedJpeg(image)
        }
    }

    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        picker.dismiss(animated: true)
        if let image = info[.originalImage] as? UIImage {
            emitCompressedJpeg(image)
        } else {
            photoCallback?(nil, nil, nil)
            photoCallback = nil
        }
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        photoCallback?(nil, nil, nil)
        photoCallback = nil
    }

    private func emitCompressedJpeg(_ image: UIImage) {
        let maxSide: CGFloat = 1600
        let size = image.size
        let scale = min(1.0, maxSide / max(size.width, size.height))
        let targetSize = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let resized = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        let b64 = resized.jpegData(compressionQuality: 0.8)?.base64EncodedString()
        let name = "foto_\(Int(Date().timeIntervalSince1970)).jpg"
        photoCallback?(b64, name, "image/jpeg")
        photoCallback = nil
    }

    func startVoiceRecording(onError: @escaping (String) -> Void) -> Bool {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)
            let tempUrl = FileManager.default.temporaryDirectory.appendingPathComponent("vn_\(UUID().uuidString).m4a")
            let settings: [String: Any] = [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                AVSampleRateKey: 16000,
                AVNumberOfChannelsKey: 1,
                AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue
            ]
            let recorder = try AVAudioRecorder(url: tempUrl, settings: settings)
            recorder.record()
            self.audioRecorder = recorder
            self.recordUrl = tempUrl
            self.recordStartDate = Date()
            return true
        } catch {
            onError("Gagal memulai rekaman suara: \(error.localizedDescription)")
            return false
        }
    }

    func stopVoiceRecording(callback: @escaping (String?, Int64, String?, String?) -> Void) {
        guard let recorder = audioRecorder, let url = recordUrl else {
            callback(nil, 0, nil, nil)
            return
        }
        recorder.stop()
        let durationMs = Int64(max(0, Date().timeIntervalSince(recordStartDate ?? Date()) * 1000))
        audioRecorder = nil
        recordUrl = nil
        recordStartDate = nil

        if let data = try? Data(contentsOf: url) {
            try? FileManager.default.removeItem(at: url)
            callback(data.base64EncodedString(), durationMs, "suara_\(Int(Date().timeIntervalSince1970)).m4a", "audio/mp4")
        } else {
            callback(nil, 0, nil, nil)
        }
    }

    func playVoice(base64Audio: String, fileName: String, onCompleted: @escaping () -> Void) {
        stopVoice()
        guard let data = Data(base64Encoded: base64Audio) else { onCompleted(); return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
            let player = try AVAudioPlayer(data: data)
            player.delegate = self
            audioPlayer = player
            audioCompletedCallback = onCompleted
            player.play()
        } catch {
            onCompleted()
        }
    }

    func stopVoice() {
        audioPlayer?.stop()
        audioPlayer = nil
        audioCompletedCallback = nil
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        audioPlayer = nil
        audioCompletedCallback?()
        audioCompletedCallback = nil
    }

    func shareText(title: String, text: String) {
        DispatchQueue.main.async {
            guard let vc = self.topViewController() else { return }
            let activity = UIActivityViewController(activityItems: [text], applicationActivities: nil)
            activity.popoverPresentationController?.sourceView = vc.view
            vc.present(activity, animated: true)
        }
    }

    func sharePdf(fileName: String, base64Pdf: String) {
        guard let data = Data(base64Encoded: base64Pdf) else { return }
        let tempUrl = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        try? data.write(to: tempUrl, options: .atomic)
        DispatchQueue.main.async {
            guard let vc = self.topViewController() else { return }
            let activity = UIActivityViewController(activityItems: [tempUrl], applicationActivities: nil)
            activity.popoverPresentationController?.sourceView = vc.view
            vc.present(activity, animated: true)
        }
    }

    func exportBackupJson(fileName: String, jsonText: String, onDone: @escaping (Bool, String) -> Void) {
        let tempUrl = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        do {
            try jsonText.write(to: tempUrl, atomically: true, encoding: .utf8)
            DispatchQueue.main.async {
                guard let vc = self.topViewController() else { onDone(false, "Tidak ada layar aktif"); return }
                let activity = UIActivityViewController(activityItems: [tempUrl], applicationActivities: nil)
                activity.popoverPresentationController?.sourceView = vc.view
                vc.present(activity, animated: true)
                onDone(true, fileName)
            }
        } catch {
            onDone(false, "Gagal membuat berkas cadangan.")
        }
    }

    func importBackupJson(onLoaded: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            guard let vc = self.topViewController() else { onLoaded(nil); return }
            self.importCallback = onLoaded
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: [UTType.json, UTType.plainText, UTType.data])
            picker.delegate = self
            picker.allowsMultipleSelection = false
            vc.present(picker, animated: true)
        }
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let url = urls.first else {
            importCallback?(nil)
            importCallback = nil
            return
        }
        let secured = url.startAccessingSecurityScopedResource()
        defer { if secured { url.stopAccessingSecurityScopedResource() } }
        let content = try? String(contentsOf: url, encoding: .utf8)
        importCallback?(content)
        importCallback = nil
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        importCallback?(nil)
        importCallback = nil
    }
}
