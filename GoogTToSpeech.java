package com.example.messagingstompwebsocket.Service;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;

public class GoogTToSpeech {
    final static Logger logger = LoggerFactory.getLogger(GoogTToSpeech.class);
    ResponseObserver<StreamingRecognizeResponse> responseObserver = null;
    ClientStream<StreamingRecognizeRequest> clientStream = null;
    public GoogTToSpeech() throws Exception {

        try (SpeechClient client = SpeechClient.create()) {

            responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {
                        ArrayList<StreamingRecognizeResponse> responses = new ArrayList<>();

                        public void onStart(StreamController controller) {}

                        public void onResponse(StreamingRecognizeResponse response) {
                            responses.add(response);
                        }

                        public void onComplete() {
                            logger.info("responses size {}", responses.size());
                            for (StreamingRecognizeResponse response : responses) {
                                StreamingRecognitionResult result = response.getResultsList().get(0);
                                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                                System.out.printf("Transcript : %s\n", alternative.getTranscript());
                            }
                        }

                        public void onError(Throwable t) {
                            System.out.println(t);
                        }
                    };

            clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);

            RecognitionConfig recognitionConfig =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode("en-US")
                            .setSampleRateHertz(8000)
                            .setModel("default")
                            .build();
            StreamingRecognitionConfig streamingRecognitionConfig =
                    StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).build();

            StreamingRecognizeRequest request =
                    StreamingRecognizeRequest.newBuilder()
                            .setStreamingConfig(streamingRecognitionConfig)
                            .build();
            clientStream.send(request);



            /**
            AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info targetInfo =
                    new DataLine.Info(
                            TargetDataLine.class,
                            audioFormat); // Set the system information to read from the microphone audio stream

            if (!AudioSystem.isLineSupported(targetInfo)) {
                System.out.println("Microphone not supported");
                System.exit(0);
            }
            // Target data line captures the audio stream the microphone produces.
            TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            System.out.println("Start speaking");
            long startTime = System.currentTimeMillis();
            // Audio Input Stream
            AudioInputStream audio = new AudioInputStream(targetDataLine);
            while (true) {
                long estimatedTime = System.currentTimeMillis() - startTime;
                byte[] data = new byte[6400];
                audio.read(data);
                if (estimatedTime > 30000) { // 30 seconds
                    System.out.println("Stop speaking.");
                    targetDataLine.stop();
                    targetDataLine.close();
                    break;
                }
                request =
                        StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(ByteString.copyFrom(data))
                                .build();
                logger.info("payload details from mic {} {}", request.getAudioContent());
                byteString = request.getAudioContent();
                clientStream.send(request);
            }
            **/
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public void send(String message) {
        try {
            JSONObject jo = new JSONObject(message);
            if (!jo.getString("event").equals("media")) {
                return;
            }

            if (jo.getString("event").equals("start")) {
                logger.info(" start details {}", jo.getJSONObject("start").getJSONObject("media_format"));
            }
            String payload = jo.getJSONObject("media").getString("payload");
            byte[] data;
            data = Base64.getDecoder().decode(payload);
            StreamingRecognizeRequest request =
                    StreamingRecognizeRequest.newBuilder()
                            .setAudioContent(ByteString.copyFrom(data))
                            .build();
            logger.info("payload details from send {} {}", request.getAudioContent());
            clientStream.send(request);

        } catch (JSONException e) {
            logger.error("Unrecognized JSON");
            e.printStackTrace();
        }

    }

    public void close() {
        logger.info("Closed");
        responseObserver.onComplete();
    }
}
