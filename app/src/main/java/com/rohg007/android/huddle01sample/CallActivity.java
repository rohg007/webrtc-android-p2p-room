package com.rohg007.android.huddle01sample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import io.socket.client.IO;
import io.socket.client.Socket;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rohg007.android.huddle01sample.models.RoomConnection;
import com.rohg007.android.huddle01sample.viewmodels.CameraViewModel;
import com.rohg007.android.huddle01sample.viewmodels.RoomConnectionViewModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;

import static com.rohg007.android.huddle01sample.Constants.AUDIO_TRACK_ID;
import static com.rohg007.android.huddle01sample.Constants.FPS;
import static com.rohg007.android.huddle01sample.Constants.RC_CALL;
import static com.rohg007.android.huddle01sample.Constants.SOCKET_URL;
import static com.rohg007.android.huddle01sample.Constants.STUN_SERVER_URL;
import static com.rohg007.android.huddle01sample.Constants.VIDEO_RESOLUTION_HEIGHT;
import static com.rohg007.android.huddle01sample.Constants.VIDEO_RESOLUTION_WIDTH;
import static com.rohg007.android.huddle01sample.Constants.VIDEO_TRACK_ID;
import static io.socket.client.Socket.EVENT_CONNECT;
import static io.socket.client.Socket.EVENT_DISCONNECT;
import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

public class CallActivity extends AppCompatActivity {

    private SurfaceViewRenderer yourSurfaceView, friendSurfaceView;
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private MediaConstraints audioConstraints;
    private AudioSource audioSource;
    private VideoTrack videoTrackFromCamera;
    private AudioTrack localAudioTrack;

    private Socket socket;
    private boolean isInitiator;
    private boolean isChannelReady;
    private boolean isStarted;

    private String roomName;

    private PeerConnection peerConnection;
    private RoomConnectionViewModel roomConnectionViewModel;
    private CameraViewModel cameraViewModel;

    private TextView yourNameTv;
    private TextView friendNameTv;

    private static final String TAG = CallActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        roomName = getIntent().getExtras().getString("roomName");
        ActionBar toolbar = getSupportActionBar();
        assert toolbar != null;
        toolbar.setTitle(roomName);
        FloatingActionButton shareButton = findViewById(R.id.share_room);
        shareButton.setOnClickListener(view -> shareRoom());
        roomConnectionViewModel = new ViewModelProvider(this).get(RoomConnectionViewModel.class);
        yourSurfaceView = findViewById(R.id.your_surface);
        friendSurfaceView = findViewById(R.id.friend_surface);
        yourNameTv = findViewById(R.id.your_name_tv);
        friendNameTv = findViewById(R.id.friend_name_tv);
        roomConnectionViewModel.getDataFromFirebase(roomName).observe(this, this::setParticipantNames);

        ExtendedFloatingActionButton hangupButton = findViewById(R.id.hangup_button);
        hangupButton.setOnClickListener(view -> disconnectCall());

        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        go();
    }

    private void setParticipantNames(RoomConnection roomConnection){
        if(roomConnection!=null){
            if(roomConnection.getParticipant1()!=null){
                if(!roomConnection.getParticipant1().getId().equals(Build.ID)) {
                    yourNameTv.setText(roomConnection.getParticipant1().getDeviceName());
                    if(roomConnection.getParticipant2()!=null){
                        friendNameTv.setText(roomConnection.getParticipant2().getDeviceName());
                    }
                }
            }
            if(roomConnection.getParticipant2()!=null){
                if(!roomConnection.getParticipant2().getId().equals(Build.ID)) {
                    yourNameTv.setText(roomConnection.getParticipant2().getDeviceName());
                    if(roomConnection.getParticipant1()!=null){
                        friendNameTv.setText(roomConnection.getParticipant1().getDeviceName());
                    }
                }
            }
        }
    }

    // Starting point, contains core sequence, requires CALL permissions before execution
    @AfterPermissionGranted(RC_CALL)
    private void go(){
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            establishServerConnection();
            initSurfaceViews();
            setPeerConnectionFactory();
            displayLocalVideoTrack();
            initPeerConnection();
            addStreamToPeerConnection();
        } else {
            EasyPermissions.requestPermissions(this, "Need some permissions", RC_CALL, perms);
        }
    }

    // Executed when required permissions are granted/denied by user
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    // initializes video rendering views, Uses EglBase surfaces for rendering
    private void initSurfaceViews(){
        eglBase = EglBase.create();
        yourSurfaceView.init(eglBase.getEglBaseContext(), null);
        yourSurfaceView.setEnableHardwareScaler(true);
        yourSurfaceView.setMirror(true);

        friendSurfaceView.init(eglBase.getEglBaseContext(), null);
        friendSurfaceView.setEnableHardwareScaler(true);
        friendSurfaceView.setMirror(true);
    }

    // renders video stream from your camera to your surface
    private void displayLocalVideoTrack() {
        audioConstraints = new MediaConstraints();
        VideoCapturer videoCapturer = createVideoCapturer();
        VideoSource videoSource = factory.createVideoSource(videoCapturer);
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addRenderer(new VideoRenderer(yourSurfaceView));

        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
    }

    // creates video capturer from camera
    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    // selects appropriate camera from device
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    // checks if camera2 APIs are supported
    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    // establishes connection with the signalling server, connects to the socket
    private void establishServerConnection() {
        try {
            socket = IO.socket(SOCKET_URL);
            setSignallingServerEventActions();
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // sets up events that should take place when a socket action is invoked
    private void setSignallingServerEventActions(){
        socket.on(EVENT_CONNECT, args -> {
            socket.emit("create or join", roomName);
        }).on("created", args -> {
            showToast("Created a new Room");
            roomConnectionViewModel.addDataToFirebase(roomName, Build.MODEL);
            isInitiator = true;
        }).on("full", args -> {
            showToast("Room is already full, Get Out");
        }).on("join", args -> {
            showToast("Peer attempting to join room");
            isChannelReady = true;
        }).on("joined", args -> {
            showToast("Peer joined the room");
            roomConnectionViewModel.addDataToFirebase(roomName, Build.MODEL);
            isChannelReady = true;
        }).on("log", args -> {
            for (Object arg : args) {
                Log.d(TAG, "connectToSignallingServer: " + String.valueOf(arg));
            }
        }).on("message", args -> {
            try {
                if (args[0] instanceof String) {
                    String message = (String) args[0];
                    if (message.equals("got user media")) {
                        // if the media stream is recieved, attempt to start a connection
                        performStartOnInitiator();
                    } else if(message.equals("bye")) {
                        // if the remote user hung up the call, your side hangs up too
                        showToast("Call Hung Up!");
                        disconnectCall();
                    }
                } else {
                    JSONObject message = (JSONObject) args[0];
                    if (message.getString("type").equals("offer")) {
                        if (!isInitiator && !isStarted) {
                            performStartOnInitiator();
                        }
                        // attempt to start a session as remote peer
                        peerConnection.setRemoteDescription(new CustomSDPObserver(), new SessionDescription(OFFER, message.getString("sdp")));
                        sendAnswer();
                    } else if (message.getString("type").equals("answer") && isStarted) {
                        // if answer is recieved, set description for remote client to current peer connection
                        peerConnection.setRemoteDescription(new CustomSDPObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
                    } else if (message.getString("type").equals("candidate") && isStarted) {
                        // if a new ice candidate comes, add new ice candidate to peer connection
                        IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                        peerConnection.addIceCandidate(candidate);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).on(EVENT_DISCONNECT, args -> {
            // when socket disconnects, remove current room data from firebase
            roomConnectionViewModel.removeDataFromFirebase(roomName);
        });
    }

    // sends answer message
    private void sendAnswer() {
        peerConnection.createAnswer(new CustomSDPObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new CustomSDPObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "answer");
                    message.put("sdp", sessionDescription.description);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    // checks if user is initiator and channel is ready
    private void performStartOnInitiator() {
        if (!isStarted && isChannelReady) {
            isStarted = true;
            if (isInitiator) {
                initCallByOffer();
            }
        }
    }

    // initializes a new call by initiating an offer message
    private void initCallByOffer() {
        MediaConstraints sdpMediaConstraints = new MediaConstraints();

        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new CustomSDPObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new CustomSDPObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "offer");
                    message.put("sdp", sessionDescription.description);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, sdpMediaConstraints);
    }

    // socket emits a message event
    private void sendMessage(Object message) {
        socket.emit("message", roomName, message);
    }

    // adds the local audio/video stream to peer connection
    private void addStreamToPeerConnection() {
        MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(videoTrackFromCamera);
        mediaStream.addTrack(localAudioTrack);
        peerConnection.addStream(mediaStream);
        sendMessage("got user media");
    }


    private void setPeerConnectionFactory() {
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        factory = new PeerConnectionFactory(null);
        factory.setVideoHwAccelerationOptions(eglBase.getEglBaseContext(), eglBase.getEglBaseContext());
    }

    private void initPeerConnection() {
        peerConnection = createPeerConnectionFromFactory(factory);
    }

    // creates a new peer connection
    private PeerConnection createPeerConnectionFromFactory(PeerConnectionFactory factory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer(STUN_SERVER_URL));

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints pcConstraints = new MediaConstraints();

        return factory.createPeerConnection(rtcConfig, pcConstraints, getPeerConnectionObeserver());
    }

    // observes on peer connection change events and performs actions
    private PeerConnection.Observer getPeerConnectionObeserver(){
        PeerConnection.Observer observer = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                // if a new ice candidate comes, sends a new message with type "candidate"
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "candidate");
                    message.put("label", iceCandidate.sdpMLineIndex);
                    message.put("id", iceCandidate.sdpMid);
                    message.put("candidate", iceCandidate.sdp);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                // if new remote stream is added to peer connection, render it on friendSurfaceView
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(true);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addRenderer(new VideoRenderer(friendSurfaceView));
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
            }

            @Override
            public void onRenegotiationNeeded() {
            }
        };
        return observer;
    }

    @Override
    protected void onDestroy() {
        // When call destroys, redirect to the call ended activity
        Intent intent = new Intent(this, CallEndedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        super.onDestroy();
    }

    // shares the current room code across messaging services
    private void shareRoom(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Join meeting on huddle01Sample with room name: "+roomName);
        startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private void disconnectCall(){
        // when disconnecting, always send a "bye" message to facilitate remote hangup
        if (socket != null) {
            sendMessage("bye");
            socket.disconnect();
        }
        // cleanup tasks
        peerConnection.close();
        yourSurfaceView.release();
        friendSurfaceView.release();
        finish();
    }

    private void showToast(String message){
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Toast.makeText(getApplicationContext(), "You can't go back, hangup to exit", Toast.LENGTH_SHORT).show();
    }
}