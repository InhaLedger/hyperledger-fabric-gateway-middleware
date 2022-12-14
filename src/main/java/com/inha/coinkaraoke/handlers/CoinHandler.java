package com.inha.coinkaraoke.handlers;

import com.inha.coinkaraoke.exceptions.BadRequestException;
import com.inha.coinkaraoke.gateway.GatewayUtils;
import com.inha.coinkaraoke.services.coins.dto.MintRequest;
import com.inha.coinkaraoke.services.coins.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Component
public class CoinHandler {

    private static final String CHAINCODE_NAME = "chaincode";
    private static final String CONTRACT_NAME = "AccountContract";

    private final GatewayUtils gatewayUtils;

    @NonNull
    public Mono<ServerResponse> transfer(ServerRequest request) {

        return request.bodyToMono(TransferRequest.class)
                .switchIfEmpty(Mono.error(new BadRequestException()))
                .publishOn(Schedulers.boundedElastic())
                .map(transferRequest -> {
                    String senderId = transferRequest.getSenderId();
                    String receiverId = transferRequest.getReceiverId();
                    String amounts = String.valueOf(transferRequest.getAmounts());

                    Contract contract = gatewayUtils.getConnection(senderId, CHAINCODE_NAME, CONTRACT_NAME);
                    return gatewayUtils.submit(contract, "transfer", receiverId, String.valueOf(new Date().getTime()), amounts);
                })
                .flatMap(submitResult->ServerResponse.ok().body(submitResult, byte[].class))
                .onErrorStop();
    }

    /**
     * This api must be strictly restricted from users. This api mints new coins to blockchain system.
     * Minting process can also be automated. Meanwhile, in this demo-application, we will not seriously
     * consider the security of the blockchain system for demonstration simplicity.
     * @param request JSON request which is the same shape with {@link MintRequest}
     */
    @NonNull
    public Mono<ServerResponse> mint(ServerRequest request) {

        return request.bodyToMono(MintRequest.class)
                .switchIfEmpty(Mono.error(new BadRequestException()))
                .publishOn(Schedulers.boundedElastic())
                .map(mintRequest -> {
                    String amounts = String.valueOf(mintRequest.getAmounts());
                    String userId = mintRequest.getUserId();

                    Contract contract = gatewayUtils.getConnection(userId, CHAINCODE_NAME, CONTRACT_NAME);
                    return gatewayUtils.submit(contract, "mint", amounts);
                })
                .flatMap(submitResult -> ServerResponse.ok().body(submitResult, byte[].class))
                .onErrorStop();
    }

}
