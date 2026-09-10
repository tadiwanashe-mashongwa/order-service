FROM quay.io/keycloak/keycloak:26.0.7

# The SpareLink realm is versioned alongside the service that owns its
# authentication contract.  Keycloak imports it when the container starts.
COPY sparelink-realm.json /opt/keycloak/data/import/sparelink-realm.json

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--import-realm"]
