HOW IT WORKS?

Pricer is standalone service which listens for SQS queues and answers into SNS topic, the main and the only responsibility 
of which is to calculate bid price for a bidder. It receives bid responses which bidder produces, win/loss notices, matches 
it with bid responses, accumulates it, makes decisions, and issues price changing directives.

incoming SQS queues:
* Bid responses
* Win notices
* Loss notices
* Currency rates

outgoing SNS topic
* Directives

BidEvidence - kind of view for BidResponse
Directive - decision for a bidder issued by pricer
Guideline - time-finite strategy which unites several directives

Bid response on receive would be converted into BidEvidence model, with only required fields and written into redis 
CacheData by requestId key. On a win/loss notice receive it would be matched with existing CacheData by requestId to 
get a context of BidEvidence.

CacheData is a container for BidEvidence and Guideline, which should be matched by requestId (bid request id)

Guidelines may be of two types: MAXIMISE_WINS which tries to increase wins by increasing price, and MINIMISE_COSTS which 
tries to keep same wins amount cheaper. Having this two types helps to guarantee only ona active guideline exists at the
moment because actions they produce are opposite.
MAXIMISE_WINS 
    - is triggered by loss_notice and starts only when no MINIMISE_COSTS guideline exists
    - increases price while there is price increasing capacity, or until a win isn't achieved 
    - in case of max price reached it became CANCELLED
    - in case of win become COMPLETE

MINIMISE_COSTS
    - is triggered by a win_notice and starts only when no ACTIVE MAXIMISE_WINS guideline exists
