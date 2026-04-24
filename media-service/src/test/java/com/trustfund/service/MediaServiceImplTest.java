package com.trustfund.service;

import com.trustfund.model.Media;
import com.trustfund.model.enums.MediaType;
import com.trustfund.model.request.MediaUploadRequest;
import com.trustfund.model.request.RegisterMediaRequest;
import com.trustfund.model.request.UpdateMediaRequest;
import com.trustfund.model.response.MediaFileResponse;
import com.trustfund.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private SupabaseStorageService supabaseStorageService;

    @InjectMocks private MediaServiceImpl service;

    private Media media;

    @BeforeEach
    void setUp() {
        media = Media.builder().id(1L).url("http://x/u.png").mediaType(MediaType.PHOTO)
                .status("ACTIVE").fileName("u.png").contentType("image/png").sizeBytes(100L).build();
    }

    @Test @DisplayName("uploadMedia_autoDetectsImage")
    void upload_image() throws IOException, InterruptedException {
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", "data".getBytes());
        MediaUploadRequest req = MediaUploadRequest.builder().file(file).build();
        when(supabaseStorageService.uploadFile(any())).thenReturn(
                new SupabaseStorageService.StoredFile("u.png", "u.png", "http://x/u.png"));
        when(mediaRepository.save(any())).thenAnswer(i -> { Media m = i.getArgument(0); m.setId(1L); return m; });
        MediaFileResponse r = service.uploadMedia(req);
        assertThat(r.getMediaType()).isEqualTo(MediaType.PHOTO);
    }

    @Test @DisplayName("uploadMedia_autoDetectsVideo")
    void upload_video() throws IOException, InterruptedException {
        MockMultipartFile file = new MockMultipartFile("file", "x.mp4", "video/mp4", "data".getBytes());
        MediaUploadRequest req = MediaUploadRequest.builder().file(file).build();
        when(supabaseStorageService.uploadFile(any())).thenReturn(
                new SupabaseStorageService.StoredFile("u.mp4", "u.mp4", "http://x/u.mp4"));
        when(mediaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        MediaFileResponse r = service.uploadMedia(req);
        assertThat(r.getMediaType()).isEqualTo(MediaType.VIDEO);
    }

    @Test @DisplayName("uploadMedia_unknownTypeFallsBackToFile")
    void upload_unknown() throws IOException, InterruptedException {
        MockMultipartFile file = new MockMultipartFile("file", "x.bin", null, "data".getBytes());
        MediaUploadRequest req = MediaUploadRequest.builder().file(file).build();
        when(supabaseStorageService.uploadFile(any())).thenReturn(
                new SupabaseStorageService.StoredFile("u.bin", "u.bin", "http://x/u.bin"));
        when(mediaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        MediaFileResponse r = service.uploadMedia(req);
        assertThat(r.getMediaType()).isEqualTo(MediaType.FILE);
    }

    @Test @DisplayName("getMediaById_notFound_throws")
    void getById_notFound() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMediaById(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("getMediaById_ok")
    void getById_ok() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        assertThat(service.getMediaById(1L).getId()).isEqualTo(1L);
    }

    @Test @DisplayName("getMediaByPostId_returnsList")
    void byPost() {
        when(mediaRepository.findByPostId(10L)).thenReturn(List.of(media));
        assertThat(service.getMediaByPostId(10L)).hasSize(1);
    }

    @Test @DisplayName("getMediaByCampaignId_returnsList")
    void byCampaign() {
        when(mediaRepository.findByCampaignId(10L)).thenReturn(List.of(media));
        assertThat(service.getMediaByCampaignId(10L)).hasSize(1);
    }

    @Test @DisplayName("getFirstImageByCampaignId_returnsNullIfEmpty")
    void firstImage_empty() {
        when(mediaRepository.findFirstByCampaignIdAndMediaTypeOrderByCreatedAtAsc(1L, MediaType.PHOTO))
                .thenReturn(Optional.empty());
        assertThat(service.getFirstImageByCampaignId(1L)).isNull();
    }

    @Test @DisplayName("getFirstImageByCampaignId_returnsFirst")
    void firstImage_ok() {
        when(mediaRepository.findFirstByCampaignIdAndMediaTypeOrderByCreatedAtAsc(1L, MediaType.PHOTO))
                .thenReturn(Optional.of(media));
        assertThat(service.getFirstImageByCampaignId(1L)).isNotNull();
    }

    @Test @DisplayName("updateMedia_updatesDescription")
    void update_ok() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(mediaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        UpdateMediaRequest req = new UpdateMediaRequest();
        req.setDescription("new desc");
        MediaFileResponse r = service.updateMedia(1L, req);
        assertThat(r.getDescription()).isEqualTo("new desc");
    }

    @Test @DisplayName("registerMedia_savesWithDefault")
    void register_ok() {
        RegisterMediaRequest req = new RegisterMediaRequest();
        req.setUrl("http://y"); req.setMediaType(MediaType.PHOTO);
        when(mediaRepository.save(any())).thenAnswer(i -> { Media m = i.getArgument(0); m.setId(2L); return m; });
        assertThat(service.registerMedia(req).getId()).isEqualTo(2L);
    }

    @Test @DisplayName("updateMediaStatus_notFound_throws")
    void updateStatus_notFound() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateMediaStatus(1L, "DELETED")).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("updateMediaStatus_ok")
    void updateStatus_ok() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(mediaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.updateMediaStatus(1L, "DELETED").getStatus()).isEqualTo("DELETED");
    }

    @Test @DisplayName("deleteMedia_callsSupabaseAndRepo")
    void delete_ok() throws Exception {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        service.deleteMedia(1L);
        verify(supabaseStorageService).deleteFileByPublicUrl(any());
        verify(mediaRepository).delete(media);
    }

    @Test @DisplayName("unlinkFromPost_setsPostIdNull")
    void unlink() {
        media.setPostId(99L);
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        service.unlinkFromPost(1L);
        assertThat(media.getPostId()).isNull();
    }
}
