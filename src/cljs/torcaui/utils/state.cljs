(ns torcaui.utils.state
  (:require [torcaui.state :as state]
            [torcaui.utils.seq :refer [find-index]])
  (:require-macros [torcaui.utils :refer [inspect]]))

(defn set-dashboard-crumbs [state {:keys [org repo branch vcs_type]}]
  (assoc-in state state/crumbs-path
            (vec (concat
                   [{:type :dashboard}]
                   (when org [{:type :org
                               :username org
                               :vcs_type vcs_type}])
                   (when repo [{:type :project
                                :username org :project repo
                                :vcs_type vcs_type}])
                   (when branch [{:type :project-branch
                                  :username org :project repo :branch branch
                                  :vcs_type vcs_type}])))))

(defn reset-current-esmt-object [state]
  (assoc state :current-esmt-data {:esmt-object {}}))

(defn reset-current-project [state]
  (assoc state :current-project-data {:project nil
                                      :plan nil
                                      :settings {}
                                      :tokens nil
                                      :checkout-keys nil
                                      :envvars nil}))

(defn reset-current-org [state]
  (assoc state :current-org-data {:plan nil
                                  :projects nil
                                  :users nil
                                  :name nil}))


(defn clear-page-state [state]
  (-> state
      (assoc :crumbs nil)
      (assoc-in state/inputs-path nil)
      (assoc-in state/error-message-path nil)
      (assoc-in state/general-message-path nil)
      (assoc-in state/page-scopes-path nil)))

(defn merge-inputs [defaults inputs keys]
  (merge (select-keys defaults keys)
         (select-keys inputs keys)))
