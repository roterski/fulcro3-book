(ns app.mutations
  (:require [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation delete-person
  [{list-id :person-list/id
    person-id :person/id}]
  (action [{:keys [state]}]
    (swap! state merge/remove-ident* [:person/id person-id] [:person-list/id list-id :person-list/people]))
  (remote [env] true))
