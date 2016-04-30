(ns torcaui.state)

(def debug-state)

(defn initial-state []
  {:error-message nil
   :general-message nil
   :environment "development"
   :navigation-point nil
   :navigation-settings nil
   :navigation-data {}
   :oauth-token {}
   :current-user nil
   :crumbs nil
   :hamburger-menu "closed"
   :orca-legacy-host nil
   ;; This isn't passed to the components, it can be accessed though om/get-shared :_app-state-do-not-use
   :inputs nil

   :current-esmt-data {:esmt-object nil}})

(def user-path [:current-user])
(def user-token-path [:current-user :oauth-token])
(def user-access-token-path [:current-user :oauth-token :access_token])

(def container-data-path [:current-build-data :container-data])
(def containers-path [:current-build-data :container-data :containers])
(def current-container-path [:current-build-data :container-data :current-container-id])
(def current-container-filter-path [:current-build-data :container-data :current-filter])
(def container-paging-offset-path [:current-build-data :container-data :paging-offset])

(def current-esmt-data [:current-esmt-data])
(def current-esmt-object [:current-esmt-data :esmt-object])

(def inner?-path [:navigation-data :inner?])

(def crumbs-path [:crumbs])

(def inputs-path [:inputs])

(def error-message-path [:error-message])
(def general-message-path [:general-message])

(def page-scopes-path [:page-scopes])