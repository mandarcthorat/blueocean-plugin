package io.jenkins.blueocean.security.authentication;

import com.google.inject.Inject;
import io.jenkins.blueocean.api.authentication.AuthenticateRequest;
import io.jenkins.blueocean.api.authentication.AuthenticateResponse;
import io.jenkins.blueocean.api.authentication.AuthenticationService;
import io.jenkins.blueocean.api.profile.model.UserDetails;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.commons.ServiceException.NotFoundException;
import io.jenkins.blueocean.security.AuthenticationProvider;
import io.jenkins.blueocean.security.Identity;
import io.jenkins.blueocean.security.UserPrototype;

import javax.annotation.Nonnull;

public class AuthenticationServiceImpl implements AuthenticationService {

    @Inject
    public AuthenticationServiceImpl() {
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public AuthenticateResponse authenticate(Identity identity, AuthenticateRequest req) {
        AuthenticationProvider provider = AuthenticationProvider.getForType(req.authType);
        if (provider == null) {
            throw new ServiceException.UnprocessableEntityException(req.authType + " is unavailable");
        }
        UserPrototype userPrototype = provider.validate(req.credentials);
        UserDetails userDetails = null;
        try {
// KK: conversation today in the chat room made me believe we can break this for now
//            userDetails = profiles.getUserDetails(Identity.ROOT, GetUserDetailsRequest.byCredentials(req.credentials)).userDetails;
        } catch (NotFoundException e) {
            userDetails = null;
        }
        return new AuthenticateResponse(userPrototype, userDetails);
    }
}
