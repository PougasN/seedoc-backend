package com.middle.api.service;

import com.middle.api.entity.Finding;
import com.middle.api.repository.FindingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindingService {

    @Autowired
    private FindingRepository findingRepository;

    public Finding saveFinding(Finding finding) {
        return findingRepository.save(finding);
    }

    public List<Finding> getFindingsByMediaId(Long mediaId) {
        return findingRepository.findByMediaId(mediaId);
    }
}
