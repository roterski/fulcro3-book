(ns app.resolvers
    (:require
      [com.wsscode.pathom.core :as p]
      [com.wsscode.pathom.connect :as pc]))

(def people-table
  (atom
    {1 {:person/id 1 :person/name "Sally" :person/age 32}
     2 {:person/id 2 :person/name "Joe" :person/age 22}
     3 {:person/id 3 :person/name "Fred" :person/age 11}
     4 {:person/id 4 :person/name "Bobby" :person/age 55}}))

(def list-table
  (atom
    {:friends {:person-list/id     :friends
               :person-list/label  "Friends"
               :person-list/people [1 2]}
     :enemies {:person-list/id     :enemies
               :person-list/label  "Enemies"
               :person-list/people [4 3]}}))

;; Given :person/id, this can generate the details of a person
(pc/defresolver person-resolver [env {:person/keys [id]}]
  {::pc/input  #{:person/id}
   ::pc/output [:person/name :person/age]}
  (get @people-table id))

;; Given a :person-list/id, this can generate a list label and the people
;; in that list (but just with their IDs)
(pc/defresolver list-resolver [env {:person-list/keys [id]}]
  {::pc/input  #{:person-list/id}
   ::pc/output [:person-list/label {:person-list/people [:person/id]}]}
  (when-let [list (get @list-table id)]
    (assoc list
      :person-list/people (mapv (fn [id] {:person/id id}) (:person-list/people list)))))

(pc/defresolver friends-resolver [env input]
  {::pc/output [{:friends [:person-list/id]}]}
  {:friends {:person-list/id :friends}})

(pc/defresolver enemies-resolver [env input]
  {::pc/output [{:enemies [:person-list/id]}]}
  {:enemies {:person-list/id :enemies}})


(def resolvers [person-resolver list-resolver friends-resolver enemies-resolver])
