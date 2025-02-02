(ns hickory-css-selectors-test
  (:require [clojure.test :refer :all]
            [hickory.core :as h]
            [hickory.select :as s]
            [hickory-css-selectors :refer :all]
            [instaparse.core :as p]))

(deftest css-selector-parser-test
  (are [css tree] (= tree (p/parse css-selector-parser css))
    ".some-class"
    [[:TOKEN [:CLASS "some-class"]]]
    ".xyz[x]"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "x"]]]
    ".xyz[x=y]"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "x" [:PRED [:ATTR_OP "="] [:VALUE "y"]]]]]
    ".xyz[x_z='y']"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "x_z" [:PRED [:ATTR_OP "="] [:VALUE "y"]]]]]
    ".xyz[4]"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "4"]]]
    "div#id"
    [[:TOKEN [:ELEM "div"] [:ID "id"]]]
    "#xyz.bold"
    [[:TOKEN [:ID "xyz"] [:CLASS "bold"]]]
    "h1.bold[title]"
    [[:TOKEN [:ELEM "h1"] [:CLASS "bold"] [:ATTR "title"]]]
    ".foo.bar.baz"
    [[:TOKEN [:CLASS "foo"] [:CLASS "bar"] [:CLASS "baz"]]]
    ".foo.bar > #foo"
    [[:TOKEN [:CLASS "foo"] [:CLASS "bar"]] [:CHILD] [:TOKEN [:ID "foo"]]]
    ".foo.bar #foo"
    [[:TOKEN [:CLASS "foo"] [:CLASS "bar"]] [:TOKEN [:ID "foo"]]]
    "body:nth-child(1)"
    [[:TOKEN [:ELEM "body"] [:NTH_CHILD [:NTH "1"]]]]
    ".foo.bar>#foo:nth-child(1)"
    [[:TOKEN [:CLASS "foo"] [:CLASS "bar"]] [:CHILD] [:TOKEN [:ID "foo"] [:NTH_CHILD [:NTH "1"]]]]
    "x > .y > #z a b > c"
    [[:TOKEN [:ELEM "x"]]
     [:CHILD]
     [:TOKEN [:CLASS "y"]]
     [:CHILD]
     [:TOKEN [:ID "z"]]
     [:TOKEN [:ELEM "a"]]
     [:TOKEN [:ELEM "b"]]
     [:CHILD]
     [:TOKEN [:ELEM "c"]]]
    "x y z"
    [[:TOKEN [:ELEM "x"]] [:TOKEN [:ELEM "y"]] [:TOKEN [:ELEM "z"]]]
    "#readme > div.Box-body.p-6 > article > p:nth-child(19)"
    [[:TOKEN [:ID "readme"]]
     [:CHILD]
     [:TOKEN [:ELEM "div"] [:CLASS "Box-body"] [:CLASS "p-6"]]
     [:CHILD]
     [:TOKEN [:ELEM "article"]]
     [:CHILD]
     [:TOKEN [:ELEM "p"] [:NTH_CHILD [:NTH "19"]]]]
    "html > body > div > li:nth-child(2)"
    [[:TOKEN [:ELEM "html"]]
     [:CHILD]
     [:TOKEN [:ELEM "body"]]
     [:CHILD]
     [:TOKEN [:ELEM "div"]]
     [:CHILD]
     [:TOKEN [:ELEM "li"] [:NTH_CHILD [:NTH "2"]]]]))

(deftest join-children-test
  (are [css tree] (= tree (join-children (p/parse css-selector-parser css) list))
    "x"
    [[:TOKEN [:ELEM "x"]]]
    "x y z"
    [[:TOKEN [:ELEM "x"]] [:TOKEN [:ELEM "y"]] [:TOKEN [:ELEM "z"]]]
    "x y > z a > b c"
    '[[:TOKEN [:ELEM "x"]]
      ([:TOKEN [:ELEM "y"]] [:TOKEN [:ELEM "z"]])
      ([:TOKEN [:ELEM "a"]] [:TOKEN [:ELEM "b"]])
      [:TOKEN [:ELEM "c"]]]
    "html > body div > li:nth-child(2)"
    '[([:TOKEN [:ELEM "html"]] [:TOKEN [:ELEM "body"]])
      ([:TOKEN [:ELEM "div"]] [:TOKEN [:ELEM "li"] [:NTH_CHILD [:NTH "2"]]])]))

(def tree
  (-> "<html><body>
        <p class='some-class other-class'>
          some text</p>
        <div id='bar' aria-name='ya'><i>italia</i>
          <ul>
           <li></li>
           <li>foo</li>
          </ul>
        </div>
        <p>2</p>
        <span id='f'>g</span>
        <div id='another-div'></div>
       </body></html>"
      (h/parse)
      (h/as-hickory)))

(defn select-css [css] (s/select (parse-css-selector css) tree))

(deftest select-test
  (is (= "bar" (-> (select-css "div#bar") first :attrs :id)))
  (is (= ["italia"] (-> (select-css "div:nth-child(2) > i") first :content)))
  (is (= ["foo"] (-> (select-css "body ul > li:nth-child(2)") first :content))))

(comment
  (s/select (parse-css-selector "div:nth-child(2) > i") tree)
  (s/select (parse-css-selector "div#bar") tree)
  (s/select (parse-css-selector "div[aria-name]") tree)
  (s/select (parse-css-selector "div[aria-namez]") tree)
  (s/select (parse-css-selector "div[aria-name^=y]") tree)
  (s/select (parse-css-selector "div[aria-name=yaz]") tree)
  (s/select (parse-css-selector "body > span[id$=f]") tree)
  (s/select (parse-css-selector "html>body>div li:nth-child(2)") tree)
  (s/select (parse-css-selector "body ul > li:nth-child(2)") tree)
  (s/select (parse-css-selector "html > body > div > li:nth-child(2)") tree)
  (s/select (parse-css-selector "div:nth-child(2) > i") tree))
