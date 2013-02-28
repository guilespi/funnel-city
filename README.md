# Welcome to Conversion Funnel City

A model for selecting what landing page to show a user who comes into a site given some information about the user, in order to 
maximize signup conversions. 

For each user landing page load, get the referer domain and a boolean representing whether the user has ever visited some site page before
(determined by a cookie when a user clicks on a shared link). For each page load, a 
choice of copy displayed above the signup is given. Also whether the user clicked 'signup' on that page load will be recorded.

This is represented as a json object:

```javascript
    {referer_domain: 'a.com', visited_share_page: false, copy_choices: ['a','b','d']}
```

The code selects one of the elements of copy_choices and then is told for this example whether the user signed up or not from that page load. 
The model is updated after each choice is made and the outcome observed.

A synthetic data file is provided. Each line is an example represented as json. 

Each example is a `[datum, choice_outcomes]` pair. Each datum is as the object above. 

There are 25 distinct `referer_domain` and ten possible `copy_choices`, although these specific numbers isn't important to the code. 
The number could even dynamically change over the stream of examples and the code should be alright.

The `choice_outcomes` data is a json object from each `copy_choices` to a boolean of whether for this datum, the user signed up or not from this page 
load given a copy choice. 

Note: The code only reads the value for `choice_outcomes` that the model selects. 

In other words, we're simulating what would happen if this were a live running experiment on the website. 
You wouldn't get to know outcomes for actions you didn't take.

## License

Copyright © 2012 Guillermo Winkler

Distributed under the Eclipse Public License, the same as Clojure.
