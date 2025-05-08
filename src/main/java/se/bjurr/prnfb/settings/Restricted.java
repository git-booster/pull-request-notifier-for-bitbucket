package se.bjurr.prnfb.settings;

import java.util.Optional;

public interface Restricted {

  Optional<String> getRepositorySlug();

  Optional<String> getProjectKey();
}
