(ns mail.mailbox-test
  (:require [clojure.test :refer [deftest is]]
            [mail.mailbox :as mailbox]
            [mail.message :as message]))

(deftest mailbox-thread-label-and-quota-state
  (let [m1 (message/message {:from "a@example.com" :to ["user@acme.example"]
                             :subject "Project" :text "hello"})
        m2 (message/message {:from "a@example.com" :to ["user@acme.example"]
                             :subject "Re: Project" :text "reply"})
        box (-> (mailbox/mailbox "mb-1" "user@acme.example")
                (mailbox/deliver (mailbox/message-entry "m1" "t1" m1
                                                        {:attachments [{:object-ref "inbound/a/0"
                                                                        :filename "brief.pdf"}]}))
                (mailbox/deliver (mailbox/message-entry "m1" "t1" m1 {}))
                (mailbox/deliver (mailbox/message-entry "m2" "t1" m2 {:read? true}))
                (mailbox/add-label "m1" :customer)
                (mailbox/trash "m2"))]
    (is (= 2 (count (:mailbox/messages box))) "delivery is idempotent")
    (is (= 2 (count (mailbox/thread box "t1"))))
    (is (= 1 (mailbox/unread-count box)))
    (is (= 10 (:mailbox/used-bytes box)))
    (is (= "inbound/a/0" (get-in box [:mailbox/messages "m1"
                                      :mailbox.message/attachments 0 :object-ref])))
    (is (= #{:inbox :customer} (get-in box [:mailbox/messages "m1" :mailbox.message/labels])))
    (is (= #{:trash} (get-in box [:mailbox/messages "m2" :mailbox.message/labels])))))

(deftest mailbox-searches-content-and-filters
  (let [msg (message/message {:from "sender@example.com" :to ["user@acme.example"]
                              :subject "Quarterly roadmap" :text "Launch Osaka"})
        box (mailbox/deliver (mailbox/mailbox "mb" "user@acme.example")
                             (mailbox/message-entry "m1" "t1" msg {:labels #{:inbox}}))]
    (is (= ["m1"] (mapv :mailbox.message/id (mailbox/search box "OSAKA" {}))))
    (is (= 1 (count (mailbox/search box "roadmap" {:label :inbox :unread? true}))))
    (is (empty? (mailbox/search box "roadmap" {:unread? false})))))
