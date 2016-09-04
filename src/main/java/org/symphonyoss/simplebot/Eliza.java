/*
 *
 *
 * Copyright 2016 Symphony Communication Services, LLC
 *
 * Licensed to Symphony Communication Services, LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.symphonyoss.simplebot;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.SymphonyClientFactory;
import org.symphonyoss.client.model.Room;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.client.services.RoomListener;
import org.symphonyoss.client.services.RoomMessage;
import org.symphonyoss.client.services.RoomService;
import org.symphonyoss.client.util.MlMessageParser;
import org.symphonyoss.symphony.agent.model.*;
import org.symphonyoss.symphony.clients.AuthorizationClient;
import org.symphonyoss.symphony.clients.DataFeedClient;
import org.symphonyoss.symphony.pod.model.Stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import static org.symphonyoss.simplebot.Eliza.ElizaCommand.*;


public class Eliza implements RoomListener {

    private final Logger logger = LoggerFactory.getLogger(Eliza.class);
    private SymphonyClient symClient;
    private Map<String,String> initParams = new HashMap<String,String>();
    private RoomService roomService;
    private Room elizaRoom;
    private DataFeedClient dataFeedClient;
    private Datafeed datafeed;
    private List<String> facts;
    private Random randomGenerator = new Random();


    static Set<String> initParamNames = new HashSet<String>();
    static {
        initParamNames.add("sessionauth.url");
        initParamNames.add("keyauth.url");
        initParamNames.add("pod.url");
        initParamNames.add("agent.url");
        initParamNames.add("truststore.file");
        initParamNames.add("truststore.password");
        initParamNames.add("keystore.password");
        initParamNames.add("certs.dir");
        initParamNames.add("bot.user.name");
        initParamNames.add("bot.user.email");
        initParamNames.add("room.stream");
        initParamNames.add("db.file");
    }

    public static void main(String[] args) {
        new Eliza();
        System.exit(0);
    }

    public Eliza() {
        initParams();
        initAuth();
        initRoom();
        initDB();
        initDatafeed();
        listenDatafeed();
        
    }

    private void initParams() {
        for(String initParam : initParamNames) {
            String systemProperty = System.getProperty(initParam);
            if (systemProperty == null) {
                throw new IllegalArgumentException("Cannot find system property; make sure you're using -D" + initParam + " to run Eliza");
            } else {
                initParams.put(initParam,systemProperty);
            }
        }
    }

    private void initAuth() {
        try {
            symClient = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.BASIC);

            logger.debug("{} {}", System.getProperty("sessionauth.url"),
                    System.getProperty("keyauth.url"));


            AuthorizationClient authClient = new AuthorizationClient(
                    initParams.get("sessionauth.url"),
                    initParams.get("keyauth.url"));


            authClient.setKeystores(
                    initParams.get("truststore.file"),
                    initParams.get("truststore.password"),
                    initParams.get("certs.dir") + initParams.get("bot.user.name") + ".p12",
                    initParams.get("keystore.password"));

            SymAuth symAuth = authClient.authenticate();


            symClient.init(
                    symAuth,
                    initParams.get("bot.user.email"),
                    initParams.get("agent.url"),
                    initParams.get("pod.url")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initRoom() {
        Stream stream = new Stream();
        stream.setId(initParams.get("room.stream"));

        try {
         roomService = new RoomService(symClient);

         elizaRoom = new Room();
         elizaRoom.setStream(stream);
         elizaRoom.setId(stream.getId());
         elizaRoom.setRoomListener(this);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    private void initDB() {
        final String dbFile = initParams.get("db.file");
        File file = new File(dbFile);
        try (BufferedReader br = new BufferedReader(new FileReader(dbFile))) {
            String line;
            facts = new ArrayList<>();
            while((line = br.readLine()) != null) {
                facts.add(line);
            }
        } catch (Exception exception) {
            System.out.println("File to open file "+file);
        }

    }

    public void initDatafeed() {
        dataFeedClient = symClient.getDataFeedClient();
        try {
			datafeed = dataFeedClient.createDatafeed();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private MessageSubmission getMessage(String message, MessageSubmission.FormatEnum formatEnum) {
        MessageSubmission aMessage = new MessageSubmission();
        aMessage.setFormat(formatEnum);
        aMessage.setMessage(message);
        return aMessage;
    }
    
    private V2MessageSubmission getAttachmentMessage() {
    	V2MessageSubmission message = new V2MessageSubmission();
    	List<AttachmentInfo> attachments = new ArrayList();
    	attachments.add(getAttachmentInfo());
    	message.attachments(attachments);
    	return message;
    }

    private AttachmentInfo getAttachmentInfo() {
    	AttachmentInfo attachmentInfo = new AttachmentInfo();
    	return attachmentInfo;
    }
    private void sendMessage(String message, MessageSubmission.FormatEnum formatEnum) {
        MessageSubmission messageSubmission = getMessage(message, formatEnum);
        try {
            symClient.getMessageService().sendMessage(elizaRoom, messageSubmission);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenDatafeed() {
        while (true) {
            try {
                Thread.sleep(4000);
                MessageList messages = dataFeedClient.getMessagesFromDatafeed(datafeed);
                if (messages != null) {
                    for (Message m : messages) {
                        if (!m.getFromUserId().equals(symClient.getLocalUser().getId())) {
                            processMessage(m);
                        }
                    }
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void processMessage(Message message) {
        String messageString = message.getMessage();
        if(StringUtils.isNotEmpty(messageString) && StringUtils.isNotBlank(messageString)) {
            MlMessageParser messageParser = new MlMessageParser();
            try {
                messageParser.parseMessage(messageString);
                String text = messageParser.getText();
                if (StringUtils.startsWithIgnoreCase(text, "eliza")) {
                    ElizaCommand cmd = getElizaCommand(text);
                    switch (cmd) {
                        case SAD:
                            sendMLMessage(formatURLText("https://www.youtube.com/watch?v=mLLO2mFy4MU"));
                            break;
                        case HUNGRY:
                            sendMessage("Auh? Get some food", MessageSubmission.FormatEnum.TEXT);
                            break;
                        case UNKNOWN:
                            break;
                        case HAPPY_BD:
                            sendMLMessage(formatURLText("https://www.youtube.com/watch?v=_z-1fTlSDF0"));
                            break;
                        case FACT:
                            sendFact();
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendFact() {
        int factCount = facts.size();
        if(factCount > 0) {
            int index = randomGenerator.nextInt(factCount);
            sendMLMessage(facts.get(index));
        }
    }

    private void sendMLMessage(String message) {
        sendMessage(String.format("<messageML>%s</messageML>", message), MessageSubmission.FormatEnum.MESSAGEML);
    }
    private String formatURLText(String url) {
        return String.format("<a href=\"%s\"/>", url);
    }

    private ElizaCommand getElizaCommand(String text) {
        if(StringUtils.containsIgnoreCase(text, "sad")) {
            return SAD;
        } else if (StringUtils.containsIgnoreCase(text, "happy birthday")) {
            return HAPPY_BD;
        } else if (StringUtils.containsIgnoreCase(text, "hungry")) {
            return HUNGRY;
        } else if (StringUtils.containsIgnoreCase(text, "fact")) {
            return FACT;
        } else {
            return UNKNOWN;
        }
    }


    @Override
    public void onRoomMessage(RoomMessage roomMessage) {

        Room room = roomService.getRoom(roomMessage.getId());

        if(room!=null && roomMessage.getMessage() != null)
            logger.debug("New room message detected from room: {} on stream: {} from: {} message: {}",
                    room.getRoomDetail().getRoomAttributes().getName(),
                    roomMessage.getRoomStream().getId(),
                    roomMessage.getMessage().getFromUserId(),
                    roomMessage.getMessage().getMessage()

                );

    }

    public enum ElizaCommand {
        SAD,
        HAPPY_BD,
        HUNGRY,
        FACT,
        UNKNOWN
    }
}
    

