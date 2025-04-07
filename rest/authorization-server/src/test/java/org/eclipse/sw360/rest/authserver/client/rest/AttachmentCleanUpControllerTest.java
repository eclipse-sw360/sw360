import org.eclipse.sw360.rest.resourceserver.admin.attachment.AttachmentCleanUpController;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.admin.attachment.Sw360AttachmentCleanUpService;
import org.junit.Test;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AttachmentCleanUpControllerTest {

    @Test
    public void testProcessAddsAttachmentCleanupLink() {
        RestControllerHelper restControllerHelper = mock(RestControllerHelper.class);
        Sw360AttachmentCleanUpService attachmentCleanUpService = mock(Sw360AttachmentCleanUpService.class);

        AttachmentCleanUpController controller = new AttachmentCleanUpController(restControllerHelper, attachmentCleanUpService);
        RepositoryLinksResource resource = new RepositoryLinksResource();

        RepositoryLinksResource processed = controller.process(resource);

        Link foundLink = processed.getLink("attachmentCleanUp").orElse(null);

        assertThat(foundLink).isNotNull();
        assertThat(foundLink.getHref()).contains("/api/attachmentCleanUp");
    }
}