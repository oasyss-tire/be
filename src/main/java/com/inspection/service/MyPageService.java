package com.inspection.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.ContractDTO;
import com.inspection.dto.ParticipantDTO;
import com.inspection.entity.Contract;
import com.inspection.entity.ParticipantToken;
import com.inspection.entity.User;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ParticipantTokenRepository;
import com.inspection.repository.UserRepository;
import com.inspection.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyPageService {
    
    private final ContractParticipantRepository contractParticipantRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final ParticipantTokenRepository participantTokenRepository;
    
    /**
     * 로그인한 사용자가 참여한 계약 목록을 조회합니다.
     * 
     * @param loginId 로그인 ID
     * @return 사용자가 참여한 계약 목록
     */
    @Transactional(readOnly = true)
    public List<ContractDTO> getMyContracts(String loginId) {
        User user = userRepository.findByUserId(loginId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + loginId));
        
        log.info("사용자 계약 조회: loginId={}, id={}", loginId, user.getId());
        
        try {
            // 사용자가 참여한 계약 목록 조회
            List<Contract> contracts = contractParticipantRepository.findContractsByUserId(user.getId());
            log.info("조회된 계약 수: {}", contracts.size());
            
            // ContractDTO로 변환
            List<ContractDTO> contractDTOs = contracts.stream()
                .map(contract -> new ContractDTO(contract, encryptionUtil))
                .collect(Collectors.toList());
                
            // 각 계약의 참여자에 대한 토큰 정보를 추가
            for (ContractDTO contractDTO : contractDTOs) {
                for (ParticipantDTO participantDTO : contractDTO.getParticipants()) {
                    // 현재 로그인한 사용자와 관련된 참여자인 경우에만 토큰 정보 추가
                    if (participantDTO.getUserId() != null && participantDTO.getUserId().equals(user.getId())) {
                        List<ParticipantToken> tokens = participantTokenRepository.findByParticipantId(participantDTO.getId());
                        if (tokens != null && !tokens.isEmpty()) {
                            participantDTO.setTokens(tokens.stream()
                                .map(ParticipantDTO.TokenInfo::new)
                                .collect(Collectors.toList()));
                            log.info("참여자 ID {}에 대한 토큰 {}개 추가", participantDTO.getId(), tokens.size());
                        }
                    }
                }
            }
            
            return contractDTOs;
        } catch (Exception e) {
            log.error("계약 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("계약 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
} 